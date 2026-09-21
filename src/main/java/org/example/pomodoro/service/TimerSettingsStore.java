package org.example.pomodoro.service;

import org.example.pomodoro.repository.AppSettingRepository;

public final class TimerSettingsStore {
    private static final String KEY = "timer_settings";
    private final AppSettingRepository repository;

    public TimerSettingsStore(AppSettingRepository repository) {
        this.repository = repository;
    }

    public void loadInto(PomodoroService service) {
        repository.find(KEY).ifPresent(value -> {
            try {
                String[] parts = value.split(",", -1);
                if (parts.length != 4) {
                    return;
                }
                int focus = Integer.parseInt(parts[0]);
                int shortBreak = Integer.parseInt(parts[1]);
                int longBreak = Integer.parseInt(parts[2]);
                int cycles = Integer.parseInt(parts[3]);
                if (focus < 1 || focus > 180 || shortBreak < 1 || shortBreak > 60
                        || longBreak < 1 || longBreak > 120 || cycles < 1 || cycles > 12) {
                    return;
                }
                service.updateSettings(focus, shortBreak, longBreak, cycles);
            } catch (IllegalArgumentException ignored) {
                // Keep defaults when an old or malformed value is encountered.
            }
        });
    }

    public void save(PomodoroService service) {
        repository.save(KEY, "%d,%d,%d,%d".formatted(
                service.getFocusMinutes(), service.getShortBreakMinutes(),
                service.getLongBreakMinutes(), service.getCycleBeforeLongBreak()));
    }
}
