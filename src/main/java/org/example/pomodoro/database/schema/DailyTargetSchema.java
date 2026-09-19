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
                    setting_value TEXT NOT NULL
                )
                """;

        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
