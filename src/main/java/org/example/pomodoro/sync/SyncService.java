package org.example.pomodoro.sync;

import java.util.List;

import static org.example.pomodoro.sync.LocalSyncRepository.SessionRow;

public final class SyncService {
    private static final int BATCH_SIZE = 100;
    private final LocalSyncRepository local;
    private final SupabaseApi api;
    private final AuthSessionStore sessionStore;

    public SyncService(LocalSyncRepository local, SupabaseApi api,
                       AuthSessionStore sessionStore) {
        this.local = local;
        this.api = api;
        this.sessionStore = sessionStore;
    }

    public boolean hasSavedLogin() {
        return sessionStore.load().isPresent();
    }

    public void signIn(String email, String password) {
        SupabaseApi.AuthTokens tokens = api.signIn(email, password);
        local.bindAccount(tokens.userId());
        sessionStore.save(new AuthSessionStore.SavedSession(
                tokens.userId(), tokens.refreshToken()));
        try {
            syncWithToken(tokens);
        } catch (RuntimeException exception) {
            throw new SupabaseApi.SyncException(
                    "Signed in, but sync failed: " + exception.getMessage(), exception);
        }
    }

    public void sync() {
        syncWithToken(refreshLogin());
    }

    public void uploadOnly() {
        uploadPending(refreshLogin());
    }

    private SupabaseApi.AuthTokens refreshLogin() {
        AuthSessionStore.SavedSession saved = sessionStore.load()
                .orElseThrow(() -> new IllegalStateException("Sign in to Supabase first."));
        SupabaseApi.AuthTokens tokens = api.refresh(saved.refreshToken());
        if (!saved.userId().equals(tokens.userId())) {
            throw new IllegalStateException("Supabase login belongs to another account.");
        }
        local.bindAccount(tokens.userId());
        sessionStore.save(new AuthSessionStore.SavedSession(
                tokens.userId(), tokens.refreshToken()));
        return tokens;
    }

    private void syncWithToken(SupabaseApi.AuthTokens tokens) {
        uploadPending(tokens);
        for (int offset = 0; ; offset += api.pageSize()) {
            List<SessionRow> page = api.downloadSessions(tokens.accessToken(), offset);
            local.importSessions(page);
            if (page.size() < api.pageSize()) {
                break;
            }
        }
        local.importSettings(api.downloadSettings(tokens.accessToken()));
    }

    private void uploadPending(SupabaseApi.AuthTokens tokens) {
        while (true) {
            List<SessionRow> pending = local.pendingSessions(BATCH_SIZE);
            if (pending.isEmpty()) {
                break;
            }
            api.uploadSessions(tokens.accessToken(), tokens.userId(), pending);
            local.markSessionsSynced(pending);
        }
        for (LocalSyncRepository.SettingRow row : local.pendingSettings()) {
            api.uploadSetting(tokens.accessToken(), tokens.userId(), row);
            local.markSettingSynced(row);
        }
    }
}
