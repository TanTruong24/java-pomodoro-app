package org.example.pomodoro.repository.impl;

import org.example.pomodoro.database.DatabaseConnection;
import org.example.pomodoro.model.FocusSession;
import org.example.pomodoro.model.PomodoroMode;
import org.example.pomodoro.repository.FocusSessionRepository;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class SQLiteFocusSessionRepository
        implements FocusSessionRepository {

    private final DatabaseConnection databaseConnection;

    public SQLiteFocusSessionRepository(
            DatabaseConnection databaseConnection
    ) {
        this.databaseConnection = databaseConnection;
    }

    @Override
    public void save(FocusSession session) {

        String sql = """
                INSERT INTO focus_session (
                    started_at,
                    ended_at,
                    duration_seconds,
                    mode
                )
                VALUES (?, ?, ?, ?)
                """;

        try (
                Connection connection =
                        databaseConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(
                    1,
                    session.getStartedAt().toEpochMilli()
            );

            statement.setLong(
                    2,
                    session.getEndedAt().toEpochMilli()
            );

            statement.setInt(
                    3,
                    session.getDurationSeconds()
            );

            statement.setString(4, session.getMode().name());

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<FocusSession> findBetween(
            Instant from,
            Instant to
    ) {

        String sql = """
                SELECT *
                FROM focus_session
                WHERE started_at >= ?
                AND started_at < ?
                AND mode = 'FOCUS'
                ORDER BY started_at DESC
                """;

        List<FocusSession> sessions = new ArrayList<>();

        try (
                Connection connection =
                        databaseConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(1, from.toEpochMilli());
            statement.setLong(2, to.toEpochMilli());

            ResultSet result = statement.executeQuery();

            while (result.next()) {

                sessions.add(
                        new FocusSession(
                                Instant.ofEpochMilli(
                                        result.getLong("started_at")
                                ),
                                Instant.ofEpochMilli(
                                        result.getLong("ended_at")
                                ),
                                result.getInt("duration_seconds"),
                                PomodoroMode.valueOf(
                                        result.getString("mode")
                                )
                        )
                );
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return sessions;
    }

    @Override
    public int getTotalFocusSeconds(
            Instant from,
            Instant to
    ) {

        String sql = """
                SELECT COALESCE(SUM(duration_seconds), 0)
                FROM focus_session
                WHERE started_at >= ?
                AND started_at < ?
                AND mode = 'FOCUS'
                """;

        try (
                Connection connection =
                        databaseConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(1, from.toEpochMilli());
            statement.setLong(2, to.toEpochMilli());

            ResultSet result = statement.executeQuery();

            return result.getInt(1);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getTotalProductiveSeconds(
            Instant from,
            Instant to
    ) {
        String sql = """
                SELECT COALESCE(SUM(duration_seconds), 0)
                FROM focus_session
                WHERE started_at >= ?
                AND started_at < ?
                AND mode IN ('FOCUS', 'SHORT_BREAK')
                """;

        try (
                Connection connection = databaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setLong(1, from.toEpochMilli());
            statement.setLong(2, to.toEpochMilli());

            ResultSet result = statement.executeQuery();
            return result.getInt(1);
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }
}
