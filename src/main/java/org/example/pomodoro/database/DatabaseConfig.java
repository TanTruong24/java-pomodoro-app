package org.example.pomodoro.database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class DatabaseConfig {

    private static final Path DATA_DIR = resolveDataDirectory();
    private static final Path DB_PATH =
            DATA_DIR.resolve("pomodoro.db");
    private static final Path LEGACY_DB_PATH =
            Path.of("data", "pomodoro.db").toAbsolutePath();

    public static String getUrl(){
        try {
            Files.createDirectories(DATA_DIR);
            migrateLegacyDatabase();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not create application data directory: "
                            + DATA_DIR,
                    exception
            );
        }

        return "jdbc:sqlite:" + DB_PATH.toAbsolutePath();
    }

    public static Path getDataDirectory() {
        return DATA_DIR;
    }

    private static void migrateLegacyDatabase() throws IOException {
        Path target = DB_PATH.toAbsolutePath();

        if (!target.equals(LEGACY_DB_PATH)
                && Files.notExists(target)
                && Files.isRegularFile(LEGACY_DB_PATH)) {
            Files.copy(
                    LEGACY_DB_PATH,
                    target,
                    StandardCopyOption.COPY_ATTRIBUTES
            );
        }
    }

    private static Path resolveDataDirectory() {
        String localAppData = System.getenv("LOCALAPPDATA");

        if (localAppData != null && !localAppData.isBlank()) {
            return Path.of(localAppData, "Pomodoro");
        }

        String xdgDataHome = System.getenv("XDG_DATA_HOME");

        if (xdgDataHome != null && !xdgDataHome.isBlank()) {
            return Path.of(xdgDataHome, "pomodoro");
        }

        return Path.of(
                System.getProperty("user.home"),
                ".pomodoro"
        );
    }
}
