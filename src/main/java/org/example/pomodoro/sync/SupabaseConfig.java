package org.example.pomodoro.sync;

import org.example.pomodoro.database.DatabaseConfig;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

public record SupabaseConfig(URI url, String publishableKey) {
    public static Optional<SupabaseConfig> load() {
        Properties properties = new Properties();
        Path file = DatabaseConfig.getDataDirectory().resolve("supabase.properties");
        if (Files.isRegularFile(file)) {
            try (InputStream stream = Files.newInputStream(file)) {
                properties.load(stream);
            } catch (IOException exception) {
                throw new IllegalStateException("Could not read " + file, exception);
            }
        }
        String url = value("SUPABASE_URL", "url", properties);
        String key = value("SUPABASE_PUBLISHABLE_KEY", "publishableKey", properties);
        if (url.isBlank() && key.isBlank()) {
            return Optional.empty();
        }
        if (url.isBlank() || key.isBlank()) {
            throw new IllegalStateException("Supabase URL and publishable key must both be set.");
        }
        URI uri = URI.create(url.endsWith("/") ? url.substring(0, url.length() - 1) : url);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalStateException("Supabase URL must be an HTTPS project URL.");
        }
        return Optional.of(new SupabaseConfig(uri, key));
    }

    private static String value(String environmentKey, String propertyKey,
                                Properties properties) {
        String environmentValue = System.getenv(environmentKey);
        return (environmentValue == null
                ? properties.getProperty(propertyKey, "") : environmentValue).trim();
    }
}
