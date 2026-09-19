package org.example.pomodoro.service;

import java.time.Instant;
import java.util.List;
import org.example.pomodoro.model.FocusSession;
import org.example.pomodoro.model.PomodoroMode;
import org.example.pomodoro.model.DailyFocusSummary;
import java.time.ZoneId;

public interface FocusHistoryService {

    void save(Instant startedAt, Instant endedAt, int durationSeconds);

    void save(
            Instant startedAt,
            Instant endedAt,
            int durationSeconds,
            PomodoroMode mode
    );

    int getTotalSeconds(Instant from, Instant to);

    List<FocusSession> findBetween(Instant from, Instant to);

    List<DailyFocusSummary> summarizeByDay(
            Instant from,
            Instant to,
            ZoneId zoneId
    );
}
