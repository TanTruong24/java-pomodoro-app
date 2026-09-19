package org.example.pomodoro.database.schema;

import java.sql.Connection;
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
                    duration_seconds INTEGER NOT NULL
                )
                """;

        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
