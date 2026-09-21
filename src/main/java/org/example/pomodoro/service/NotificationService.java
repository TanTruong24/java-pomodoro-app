package org.example.pomodoro.service;

import javafx.stage.Window;
import org.example.pomodoro.model.AppTheme;
import org.example.pomodoro.model.PomodoroMode;

public interface NotificationService {

    void showSessionFinished(
            Window owner,
            PomodoroMode finishedMode,
            PomodoroMode nextMode,
            int nextDurationSeconds,
            AppTheme theme
    );
}
