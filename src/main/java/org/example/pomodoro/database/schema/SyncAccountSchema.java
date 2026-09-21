package org.example.pomodoro.database.schema;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class SyncAccountSchema implements Schema {
    @Override
    public void create(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS sync_account (
                        singleton INTEGER PRIMARY KEY CHECK (singleton = 1),
                        user_id TEXT NOT NULL
                    )
                    """);
        }
    }
}
