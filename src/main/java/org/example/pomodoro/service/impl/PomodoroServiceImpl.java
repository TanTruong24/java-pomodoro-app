package org.example.pomodoro.service.impl;

import org.example.pomodoro.model.PomodoroMode;
import org.example.pomodoro.model.PomodoroSetting;

public class PomodoroServiceImpl implements org.example.pomodoro.service.PomodoroService {

    private PomodoroSetting pomodoroSetting;

    private int currentFocus = 1;

    private PomodoroMode pomodoroMode = PomodoroMode.FOCUS;

    private int completeCycles = 0;

    public PomodoroServiceImpl(PomodoroSetting pomodoroSetting) {
        this.pomodoroSetting = pomodoroSetting;
    }

    @Override
    public int getCurrentFocusNumber() {
        return currentFocus;
    }

    @Override
    public int getCycleBeforeLongBreak() {
        return pomodoroSetting.getCycleBeforeLongBreak();
    }

    @Override
    public int getCurrentDurationSeconds() {
        return switch (pomodoroMode) {
            case FOCUS -> pomodoroSetting.getFocusMinutes() * 60;
            case LONG_BREAK -> pomodoroSetting.getLongBreakMinutes() * 60;
            case SHORT_BREAK -> pomodoroSetting.getShortBreakMinutes() * 60;
        };
    }

    @Override
    public PomodoroMode getCurrentMode() {
        return pomodoroMode;
    }

    @Override
    public int getCompleteCycles() {
        return completeCycles;
    }

    @Override
    public void moveToNextMode() {

        switch (pomodoroMode) {

            case FOCUS -> {

                if (currentFocus == pomodoroSetting.getCycleBeforeLongBreak()) {
                    pomodoroMode = PomodoroMode.LONG_BREAK;
                } else {
                    pomodoroMode = PomodoroMode.SHORT_BREAK;
                }
            }

            case SHORT_BREAK -> {
                currentFocus++;
                pomodoroMode = PomodoroMode.FOCUS;
            }

            case LONG_BREAK -> {
                currentFocus = 1;
                pomodoroMode = PomodoroMode.FOCUS;
            }
        }
    }

    @Override
    public void reset() {
        pomodoroMode = PomodoroMode.FOCUS;
        currentFocus = 1;
    }
}
