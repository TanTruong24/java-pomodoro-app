package org.example.pomodoro.config;

import org.example.pomodoro.controller.PomodoroController;
import org.example.pomodoro.model.PomodoroSetting;
import org.example.pomodoro.service.PomodoroService;
import org.example.pomodoro.service.TimerService;
import org.example.pomodoro.service.impl.PomodoroServiceImpl;
import org.example.pomodoro.service.impl.TimerServiceImpl;

public class AppContext {

    private final PomodoroSetting setting;

    private final PomodoroService pomodoroService;
    private final TimerService timerService;

    public AppContext() {

        setting = new PomodoroSetting();

        pomodoroService = new PomodoroServiceImpl(setting);

        timerService = new TimerServiceImpl();
    }

    public PomodoroController createPomodoroController() {
        return new PomodoroController(
                pomodoroService,
                timerService
        );
    }
}
