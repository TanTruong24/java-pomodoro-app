package org.example.pomodoro.service.impl;

import org.example.pomodoro.repository.AppSettingRepository;
import org.example.pomodoro.repository.FocusSessionRepository;
import org.example.pomodoro.service.DailyTargetService;

import java.time.Instant;

public class DailyTargetServiceImpl implements DailyTargetService {

    private static final String TARGET_KEY = "daily_target_minutes";
    private static final int DEFAULT_TARGET_MINUTES = 4 * 60;

    private final AppSettingRepository appSettingRepository;
    private final FocusSessionRepository focusSessionRepository;

    public DailyTargetServiceImpl(
            AppSettingRepository appSettingRepository,
            FocusSessionRepository focusSessionRepository
    ) {
        this.appSettingRepository = appSettingRepository;
        this.focusSessionRepository = focusSessionRepository;
    }

    @Override
    public int getTargetMinutes() {
        return appSettingRepository.find(TARGET_KEY)
                .map(Integer::parseInt)
                .orElse(DEFAULT_TARGET_MINUTES);
    }

    @Override
    public void updateTargetMinutes(int targetMinutes) {
        if (targetMinutes <= 0 || targetMinutes > 24 * 60) {
            throw new IllegalArgumentException(
                    "Daily target must be between 1 minute and 24 hours"
            );
        }

        appSettingRepository.save(
                TARGET_KEY,
                String.valueOf(targetMinutes)
        );
    }

    @Override
    public int getTrackedSeconds(Instant from, Instant to) {
        return focusSessionRepository.getTotalProductiveSeconds(from, to);
    }
}
