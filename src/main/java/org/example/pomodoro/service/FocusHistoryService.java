package org.example.pomodoro.service;

import java.time.Instant;

public interface FocusHistoryService {

    void save(Instant startedAt, Instant endedAt, int durationSeconds);

    int getTotalSeconds(Instant from, Instant to);
}
