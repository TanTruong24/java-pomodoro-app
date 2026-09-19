package org.example.pomodoro.model;

import java.time.Instant;

public class FocusSession {

    private Long id;

    private Instant startedAt;
    private Instant endedAt;

    private int durationSeconds;

    public FocusSession(Instant startedAt, Instant endedAt, int durationSeconds) {
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.durationSeconds = durationSeconds;
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
}
