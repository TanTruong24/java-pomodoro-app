package org.example.pomodoro.sync;

import org.example.pomodoro.database.DatabaseConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

public final class AuthSessionStore {
    private final Path path = DatabaseConfig.getDataDirectory().resolve("supabase-session.properties");

    public Optional<SavedSession> load() {
        if (!Files.isRegularFile(path)) {
            return Optional.empty();
        }
        Properties properties = new Properties();
        try (InputStream stream = Files.newInputStream(path)) {
            properties.load(stream);
            return Optional.of(new SavedSession(
                    UUID.fromString(properties.getProperty("userId")),
                    properties.getProperty("refreshToken")));
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException("Could not read the saved Supabase login", exception);
        }
    }

    public void save(SavedSession session) {
        try {
            Files.createDirectories(path.getParent());
            Path temporary = Files.createTempFile(path.getParent(), "supabase-session-", ".tmp");
            try {
                restrictPermissions(temporary);
                Properties properties = new Properties();
                properties.setProperty("userId", session.userId().toString());
                properties.setProperty("refreshToken", session.refreshToken());
                try (OutputStream stream = Files.newOutputStream(temporary)) {
                    properties.store(stream, "Supabase login session; keep this file private");
                }
                try {
                    Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE);
                } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
                    Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save the Supabase login", exception);
        }
    }

    private void restrictPermissions(Path file) throws IOException {
        if (Files.getFileStore(file).supportsFileAttributeView("posix")) {
            Files.setPosixFilePermissions(file, EnumSet.of(
                    PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        }
    }

    public record SavedSession(UUID userId, String refreshToken) { }
}
