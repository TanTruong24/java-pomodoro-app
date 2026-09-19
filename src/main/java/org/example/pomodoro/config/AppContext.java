package org.example.pomodoro.config;

import org.example.pomodoro.controller.PomodoroController;
import org.example.pomodoro.database.DatabaseConnection;
import org.example.pomodoro.database.DatabaseInitializer;
import org.example.pomodoro.database.schema.FocusSessionSchema;
import org.example.pomodoro.model.PomodoroSetting;
import org.example.pomodoro.repository.FocusSessionRepository;
import org.example.pomodoro.repository.impl.SQLiteFocusSessionRepository;
import org.example.pomodoro.service.FocusHistoryService;
import org.example.pomodoro.service.PomodoroService;
import org.example.pomodoro.service.SoundService;
import org.example.pomodoro.service.TimerService;
import org.example.pomodoro.service.impl.FocusHistoryServiceImpl;
import org.example.pomodoro.service.impl.PomodoroServiceImpl;
import org.example.pomodoro.service.impl.SoundServiceImpl;
import org.example.pomodoro.service.impl.TimerServiceImpl;

import java.util.List;

public class AppContext {

    private final PomodoroSetting setting;

    private final PomodoroService pomodoroService;
    private final TimerService timerService;

    private final DatabaseConnection databaseConnection;

    private final FocusSessionRepository focusSessionRepository;

    private final FocusHistoryService focusHistoryService;

    private final SoundService soundService;


    public AppContext() {

        databaseConnection =
                new DatabaseConnection();

        DatabaseInitializer databaseInitializer =
                new DatabaseInitializer(
                        databaseConnection,
                        List.of(
                                new FocusSessionSchema()
                        )
                );

        databaseInitializer.initialize();

        setting = new PomodoroSetting();

        pomodoroService = new PomodoroServiceImpl(setting);

        timerService = new TimerServiceImpl();

        focusSessionRepository =
                new SQLiteFocusSessionRepository(
                        databaseConnection
                );

        focusHistoryService =
                new FocusHistoryServiceImpl(
                        focusSessionRepository
                );

        soundService = new SoundServiceImpl();
    }

    public PomodoroController createPomodoroController() {
        return new PomodoroController(
                pomodoroService,
                timerService,
                focusHistoryService,
                soundService
        );
    }
}
