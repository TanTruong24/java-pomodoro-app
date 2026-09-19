package org.example.pomodoro.repository;

import org.example.pomodoro.model.FocusSession;

import java.time.Instant;
import java.util.List;

public interface FocusSessionRepository {

    void save(FocusSession session);

    List<FocusSession> findBetween(Instant from, Instant to);

    int getTotalFocusSeconds(Instant from, Instant to);

    int getTotalProductiveSeconds(Instant from, Instant to);
}
