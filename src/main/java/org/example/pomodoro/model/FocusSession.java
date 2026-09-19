package org.example.pomodoro.model;

import java.time.Instant;

public class FocusSession {

    private Long id;

    private Instant startedAt;
    private Instant endedAt;

    private int durationSeconds;
    private PomodoroMode mode;

    public FocusSession(Instant startedAt, Instant endedAt, int durationSeconds) {
        this(startedAt, endedAt, durationSeconds, PomodoroMode.FOCUS);
    }

    public FocusSession(
            Instant startedAt,
            Instant endedAt,
            int durationSeconds,
            PomodoroMode mode
    ) {
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.durationSeconds = durationSeconds;
        this.mode = mode;
    }

    public Long getId() {
        return id;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public PomodoroMode getMode() {
        return mode;
    }
}
