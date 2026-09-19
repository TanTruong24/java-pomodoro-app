package org.example.pomodoro.service;

import org.example.pomodoro.model.PomodoroMode;

public interface PomodoroService {

    PomodoroMode getCurrentMode();

    int getCurrentDurationSeconds();

    int getCompleteCycles();

    void moveToNextMode();

    void reset();

    int getCurrentFocusNumber();

    int getCycleBeforeLongBreak();

    int getFocusMinutes();

    int getShortBreakMinutes();

    int getLongBreakMinutes();

    void updateSettings(
            int focusMinutes,
            int shortBreakMinutes,
            int longBreakMinutes,
            int cyclesBeforeLongBreak
    );
}
