package org.example.pomodoro.sync;

import org.example.pomodoro.database.DatabaseConnection;
import org.example.pomodoro.database.schema.FocusSessionSchema;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class LocalSyncRepository {
    private final DatabaseConnection databaseConnection;

    public LocalSyncRepository(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    public void bindAccount(UUID userId) {
        try (Connection connection = databaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement find = connection.prepareStatement(
                    "SELECT user_id FROM sync_account WHERE singleton = 1");
                 ResultSet result = find.executeQuery()) {
                if (result.next()) {
                    if (!userId.toString().equals(result.getString(1))) {
                        throw new IllegalStateException(
                                "This local database is linked to a different Supabase account.");
                    }
                } else {
                    try (PreparedStatement insert = connection.prepareStatement(
                            "INSERT INTO sync_account (singleton, user_id) VALUES (1, ?)")) {
                        insert.setString(1, userId.toString());
                        insert.executeUpdate();
                    }
                }
            }
            connection.commit();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not link the local database to an account", exception);
        }
    }

    public List<SessionRow> pendingSessions(int limit) {
        String sql = """
                SELECT sync_id, started_at, ended_at, duration_seconds, mode
                FROM focus_session WHERE synced = 0 ORDER BY id LIMIT ?
                """;
        List<SessionRow> rows = new ArrayList<>();
        try (Connection connection = databaseConnection.getConnection()) {
            FocusSessionSchema.backfillMissingSyncIds(connection);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, limit);
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        String syncId = result.getString(1);
                        if (syncId == null || syncId.isBlank()) {
                            throw new IllegalStateException("A local session has no sync ID.");
                        }
                        rows.add(new SessionRow(
                                UUID.fromString(syncId),
                                Instant.ofEpochMilli(result.getLong(2)),
                                Instant.ofEpochMilli(result.getLong(3)),
                                result.getInt(4), result.getString(5)));
                    }
                }
            }
            return rows;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load pending sessions", exception);
        }
    }

    public void markSessionsSynced(List<SessionRow> rows) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE focus_session SET synced = 1 WHERE sync_id = ?")) {
            connection.setAutoCommit(false);
            for (SessionRow row : rows) {
                statement.setString(1, row.id().toString());
                statement.addBatch();
            }
            statement.executeBatch();
            connection.commit();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not mark sessions as synced", exception);
        }
    }

    public void importSessions(List<SessionRow> rows) {
        String sql = """
                INSERT OR IGNORE INTO focus_session
                (sync_id, started_at, ended_at, duration_seconds, mode, synced)
                VALUES (?, ?, ?, ?, ?, 1)
                """;
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement insert = connection.prepareStatement(sql);
             PreparedStatement acknowledge = connection.prepareStatement(
                     "UPDATE focus_session SET synced = 1 WHERE sync_id = ?")) {
            connection.setAutoCommit(false);
            for (SessionRow row : rows) {
                insert.setString(1, row.id().toString());
                insert.setLong(2, row.startedAt().toEpochMilli());
                insert.setLong(3, row.endedAt().toEpochMilli());
                insert.setInt(4, row.durationSeconds());
                insert.setString(5, row.mode());
                insert.addBatch();
                acknowledge.setString(1, row.id().toString());
                acknowledge.addBatch();
            }
            insert.executeBatch();
            acknowledge.executeBatch();
            connection.commit();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not import remote sessions", exception);
        }
    }

    public List<SettingRow> pendingSettings() {
        List<SettingRow> rows = new ArrayList<>();
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT setting_key, setting_value FROM app_setting WHERE dirty = 1");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                rows.add(new SettingRow(result.getString(1), result.getString(2)));
            }
            return rows;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load pending settings", exception);
        }
    }

    public void markSettingSynced(SettingRow row) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE app_setting SET dirty = 0 WHERE setting_key = ? AND setting_value = ?")) {
            statement.setString(1, row.key());
            statement.setString(2, row.value());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not mark setting as synced", exception);
        }
    }

    public void importSettings(List<SettingRow> rows) {
        String sql = """
                INSERT INTO app_setting (setting_key, setting_value, dirty)
                VALUES (?, ?, 0)
                ON CONFLICT(setting_key) DO UPDATE SET
                    setting_value = excluded.setting_value,
                    dirty = 0
                WHERE app_setting.dirty = 0
                """;
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            connection.setAutoCommit(false);
            for (SettingRow row : rows) {
                statement.setString(1, row.key());
                statement.setString(2, row.value());
                statement.addBatch();
            }
            statement.executeBatch();
            connection.commit();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not import remote settings", exception);
        }
    }

    public record SessionRow(UUID id, Instant startedAt, Instant endedAt,
                             int durationSeconds, String mode) { }

    public record SettingRow(String key, String value) { }
}
