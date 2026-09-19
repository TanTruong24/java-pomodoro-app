package org.example.pomodoro.database.schema;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class FocusSessionSchema implements Schema{

    @Override
    public void create(Connection connection) throws SQLException {

        String sql = """
                CREATE TABLE IF NOT EXISTS focus_session (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    started_at INTEGER NOT NULL,
                    ended_at INTEGER NOT NULL,
                    duration_seconds INTEGER NOT NULL,
                    mode TEXT NOT NULL DEFAULT 'FOCUS'
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
        }
    }

    private boolean hasModeColumn(Statement statement) throws SQLException {
        try (ResultSet columns =
                     statement.executeQuery("PRAGMA table_info(focus_session)")) {
            while (columns.next()) {
                if ("mode".equalsIgnoreCase(columns.getString("name"))) {
                    return true;
                }
            }
        }

        return false;
    }
}
