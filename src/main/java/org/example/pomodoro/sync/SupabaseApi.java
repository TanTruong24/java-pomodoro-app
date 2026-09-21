package org.example.pomodoro.sync;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.example.pomodoro.sync.LocalSyncRepository.SessionRow;
import static org.example.pomodoro.sync.LocalSyncRepository.SettingRow;

public final class SupabaseApi {
    private static final int PAGE_SIZE = 500;
    private final SupabaseConfig config;
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3)).build();

    public SupabaseApi(SupabaseConfig config) {
        this.config = config;
    }

    public AuthTokens signIn(String email, String password) {
        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        body.addProperty("password", password);
        return auth("password", body);
    }

    public AuthTokens refresh(String refreshToken) {
        JsonObject body = new JsonObject();
        body.addProperty("refresh_token", refreshToken);
        return auth("refresh_token", body);
    }

    private AuthTokens auth(String grantType, JsonObject body) {
        HttpRequest request = baseRequest("/auth/v1/token?grant_type=" + grantType)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
        JsonObject json = JsonParser.parseString(send(request)).getAsJsonObject();
        return new AuthTokens(
                UUID.fromString(json.getAsJsonObject("user").get("id").getAsString()),
                json.get("access_token").getAsString(),
                json.get("refresh_token").getAsString());
    }

    public void uploadSessions(String accessToken, UUID userId, List<SessionRow> rows) {
        JsonArray body = new JsonArray();
        for (SessionRow row : rows) {
            JsonObject json = new JsonObject();
            json.addProperty("id", row.id().toString());
            json.addProperty("user_id", userId.toString());
            json.addProperty("started_at", row.startedAt().toString());
            json.addProperty("ended_at", row.endedAt().toString());
            json.addProperty("duration_seconds", row.durationSeconds());
            json.addProperty("mode", row.mode());
            body.add(json);
        }
        HttpRequest request = dataRequest(
                "/rest/v1/focus_sessions?on_conflict=id", accessToken)
                .header("Prefer", "resolution=ignore-duplicates,return=minimal")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
        send(request);
    }

    public List<SessionRow> downloadSessions(String accessToken, int offset) {
        HttpRequest request = dataRequest(
                "/rest/v1/focus_sessions?select=id,started_at,ended_at,duration_seconds,mode"
                        + "&order=id.asc&limit=" + PAGE_SIZE + "&offset=" + offset,
                accessToken).GET().build();
        JsonArray json = JsonParser.parseString(send(request)).getAsJsonArray();
        List<SessionRow> rows = new ArrayList<>();
        for (var element : json) {
            JsonObject item = element.getAsJsonObject();
            rows.add(new SessionRow(
                    UUID.fromString(item.get("id").getAsString()),
                    Instant.parse(item.get("started_at").getAsString()),
                    Instant.parse(item.get("ended_at").getAsString()),
                    item.get("duration_seconds").getAsInt(),
                    item.get("mode").getAsString()));
        }
        return rows;
    }

    public void uploadSetting(String accessToken, UUID userId, SettingRow row) {
        JsonObject body = new JsonObject();
        body.addProperty("user_id", userId.toString());
        body.addProperty("setting_key", row.key());
        body.addProperty("setting_value", row.value());
        HttpRequest request = dataRequest(
                "/rest/v1/app_settings?on_conflict=user_id,setting_key", accessToken)
                .header("Prefer", "resolution=merge-duplicates,return=minimal")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
        send(request);
    }

    public List<SettingRow> downloadSettings(String accessToken) {
        HttpRequest request = dataRequest(
                "/rest/v1/app_settings?select=setting_key,setting_value", accessToken)
                .GET().build();
        JsonArray json = JsonParser.parseString(send(request)).getAsJsonArray();
        List<SettingRow> rows = new ArrayList<>();
        for (var element : json) {
            JsonObject item = element.getAsJsonObject();
            rows.add(new SettingRow(item.get("setting_key").getAsString(),
                    item.get("setting_value").getAsString()));
        }
        return rows;
    }

    public int pageSize() {
        return PAGE_SIZE;
    }

    private HttpRequest.Builder dataRequest(String path, String accessToken) {
        return baseRequest(path).header("Authorization", "Bearer " + accessToken);
    }

    private HttpRequest.Builder baseRequest(String path) {
        URI uri = config.url().resolve(path);
        return HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(4))
                .header("apikey", config.publishableKey())
                .header("Content-Type", "application/json");
    }

    private String send(HttpRequest request) {
        try {
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new SyncException(errorMessage(response.statusCode(), response.body()));
            }
            return response.body();
        } catch (IOException exception) {
            throw new SyncException(connectionErrorMessage(exception), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new SyncException("Supabase request was interrupted", exception);
        }
    }

    private static String connectionErrorMessage(IOException exception) {
        if (ModuleLayer.boot().findModule("jdk.crypto.ec").isEmpty()) {
            return "The packaged Java runtime lacks jdk.crypto.ec. Rebuild with "
                    + "--add-modules jdk.crypto.ec to enable HTTPS to Supabase.";
        }
        String reason = exception.getMessage();
        String detail = exception.getClass().getSimpleName()
                + (reason == null || reason.isBlank() ? "" : ": " + reason);
        return "Could not reach Supabase (" +
                (detail.length() > 240 ? detail.substring(0, 240) : detail) + ").";
    }

    static String errorMessage(int status, String body) {
        String fallback = "Supabase returned HTTP " + status;
        if (body == null || body.isBlank()) {
            return fallback;
        }
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            for (String field : List.of("message", "error_description", "msg", "error")) {
                JsonElement value = json.get(field);
                if (value != null && value.isJsonPrimitive()
                        && value.getAsJsonPrimitive().isString()
                        && !value.getAsString().isBlank()) {
                    String message = value.getAsString().trim();
                    return message.length() > 300 ? message.substring(0, 300) : message;
                }
            }
        } catch (RuntimeException ignored) {
            // Some gateways return HTML or plain text instead of a JSON error.
        }
        return fallback;
    }

    public record AuthTokens(UUID userId, String accessToken, String refreshToken) { }

    public static final class SyncException extends RuntimeException {
        public SyncException(String message) { super(message); }
        public SyncException(String message, Throwable cause) { super(message, cause); }
    }
}
