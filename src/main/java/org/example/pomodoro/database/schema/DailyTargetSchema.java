package org.example.pomodoro.database.schema;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DailyTargetSchema implements Schema {

    @Override
    public void create(Connection connection) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS app_setting (
                    setting_key TEXT PRIMARY KEY,
                    setting_value TEXT NOT NULL,
                    dirty INTEGER NOT NULL DEFAULT 1
                )
                """;

        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
            boolean hasDirty = false;
            try (var columns = statement.executeQuery("PRAGMA table_info(app_setting)")) {
                while (columns.next()) {
                    hasDirty |= "dirty".equalsIgnoreCase(columns.getString("name"));
                }
            }
            if (!hasDirty) {
                statement.execute("ALTER TABLE app_setting ADD COLUMN dirty INTEGER NOT NULL DEFAULT 1");
            }
        }
    }
}
