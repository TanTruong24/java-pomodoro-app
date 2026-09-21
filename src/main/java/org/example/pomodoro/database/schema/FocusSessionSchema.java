package org.example.pomodoro.database.schema;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FocusSessionSchema implements Schema{

    @Override
    public void create(Connection connection) throws SQLException {

        String sql = """
                CREATE TABLE IF NOT EXISTS focus_session (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    started_at INTEGER NOT NULL,
                    ended_at INTEGER NOT NULL,
                    duration_seconds INTEGER NOT NULL,
                    mode TEXT NOT NULL DEFAULT 'FOCUS',
                    sync_id TEXT,
                    synced INTEGER NOT NULL DEFAULT 0
                )
                """;

        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);

            if (!hasModeColumn(statement)) {
                statement.execute(
                        "ALTER TABLE focus_session "
                                + "ADD COLUMN mode TEXT NOT NULL "
                                + "DEFAULT 'FOCUS'"
                );
            }
            if (!hasColumn(statement, "sync_id")) {
                statement.execute("ALTER TABLE focus_session ADD COLUMN sync_id TEXT");
            }
            if (!hasColumn(statement, "synced")) {
                statement.execute("ALTER TABLE focus_session ADD COLUMN synced INTEGER NOT NULL DEFAULT 0");
            }
        }
        backfillMissingSyncIds(connection);
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_focus_session_sync_id ON focus_session(sync_id)");
        }
    }

    public static void backfillMissingSyncIds(Connection connection) throws SQLException {
        List<Long> missingIds = new ArrayList<>();
        try (PreparedStatement missing = connection.prepareStatement(
                    "SELECT id FROM focus_session WHERE sync_id IS NULL OR trim(sync_id) = ''");
             ResultSet rows = missing.executeQuery()) {
            while (rows.next()) {
                missingIds.add(rows.getLong(1));
            }
        }
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE focus_session SET sync_id = ? WHERE id = ? "
                        + "AND (sync_id IS NULL OR trim(sync_id) = '')")) {
            for (Long id : missingIds) {
                update.setString(1, UUID.randomUUID().toString());
                update.setLong(2, id);
                update.addBatch();
            }
            update.executeBatch();
        }
    }

    private boolean hasModeColumn(Statement statement) throws SQLException {
        return hasColumn(statement, "mode");
    }

    private boolean hasColumn(Statement statement, String column) throws SQLException {
        try (ResultSet columns =
                     statement.executeQuery("PRAGMA table_info(focus_session)")) {
            while (columns.next()) {
                if (column.equalsIgnoreCase(columns.getString("name"))) {
                    return true;
                }
            }
        }

        return false;
    }
}
