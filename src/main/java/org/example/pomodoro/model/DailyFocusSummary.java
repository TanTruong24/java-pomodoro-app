package org.example.pomodoro.model;

import java.time.LocalDate;

public record DailyFocusSummary(
        LocalDate date,
        int totalSeconds,
        int sessionCount
) {
}
