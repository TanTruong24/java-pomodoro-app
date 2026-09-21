package org.example.pomodoro.config;

import org.example.pomodoro.controller.PomodoroController;
import org.example.pomodoro.controller.HistoryController;
import org.example.pomodoro.database.DatabaseConnection;
import org.example.pomodoro.database.DatabaseInitializer;
import org.example.pomodoro.database.schema.FocusSessionSchema;
import org.example.pomodoro.database.schema.DailyTargetSchema;
import org.example.pomodoro.model.PomodoroSetting;
import org.example.pomodoro.repository.FocusSessionRepository;
import org.example.pomodoro.repository.AppSettingRepository;
import org.example.pomodoro.repository.impl.SQLiteFocusSessionRepository;
import org.example.pomodoro.repository.impl.SQLiteAppSettingRepository;
import org.example.pomodoro.service.FocusHistoryService;
import org.example.pomodoro.service.DailyTargetService;
import org.example.pomodoro.service.PomodoroService;
import org.example.pomodoro.service.NotificationService;
import org.example.pomodoro.service.SoundService;
import org.example.pomodoro.service.TimerService;
import org.example.pomodoro.service.ThemeService;
import org.example.pomodoro.service.impl.FocusHistoryServiceImpl;
import org.example.pomodoro.service.impl.DailyTargetServiceImpl;
import org.example.pomodoro.service.impl.PomodoroServiceImpl;
import org.example.pomodoro.service.impl.JavaFxNotificationService;
import org.example.pomodoro.service.impl.SoundServiceImpl;
import org.example.pomodoro.service.impl.TimerServiceImpl;
import org.example.pomodoro.service.impl.ThemeServiceImpl;

import java.util.List;

public class AppContext {

    private final PomodoroSetting setting;

    private final PomodoroService pomodoroService;
    private final TimerService timerService;

    private final DatabaseConnection databaseConnection;

    private final FocusSessionRepository focusSessionRepository;
    private final AppSettingRepository appSettingRepository;

    private final FocusHistoryService focusHistoryService;
    private final DailyTargetService dailyTargetService;
    private final ThemeService themeService;

    private final SoundService soundService;
    private final NotificationService notificationService;


    public AppContext() {

        databaseConnection =
                new DatabaseConnection();

        DatabaseInitializer databaseInitializer =
                new DatabaseInitializer(
                        databaseConnection,
                        List.of(
                                new FocusSessionSchema(),
                                new DailyTargetSchema()
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

        appSettingRepository =
                new SQLiteAppSettingRepository(databaseConnection);

        focusHistoryService =
                new FocusHistoryServiceImpl(
                        focusSessionRepository
                );

        dailyTargetService =
                new DailyTargetServiceImpl(
                        appSettingRepository,
                        focusSessionRepository
                );

        themeService = new ThemeServiceImpl(appSettingRepository);

        soundService = new SoundServiceImpl();
        notificationService = new JavaFxNotificationService();
    }

    public PomodoroController createPomodoroController() {
        return new PomodoroController(
                pomodoroService,
                timerService,
                focusHistoryService,
                soundService,
                notificationService,
                themeService
        );
    }

    public HistoryController createHistoryController() {
        return new HistoryController(
                focusHistoryService,
                dailyTargetService
        );
    }
}
