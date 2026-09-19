package org.example.pomodoro.service;

import java.time.Instant;

public interface DailyTargetService {

    int getTargetMinutes();

    void updateTargetMinutes(int targetMinutes);

    int getTrackedSeconds(Instant from, Instant to);
}
