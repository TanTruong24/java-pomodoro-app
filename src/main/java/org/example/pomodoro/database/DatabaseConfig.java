package org.example.pomodoro.database;

import java.nio.file.Path;

public final class DatabaseConfig {

    private static final Path DATA_DIR = Path.of("data");
    private static final Path DB_PATH =
            DATA_DIR.resolve("pomodoro.db");

    public static String getUrl(){
        return "jdbc:sqlite:" + DB_PATH;
    }
}
