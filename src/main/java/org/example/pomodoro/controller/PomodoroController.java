package org.example.pomodoro.controller;

import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.example.pomodoro.model.AppTheme;
import org.example.pomodoro.model.PomodoroMode;
import org.example.pomodoro.service.FocusHistoryService;
import org.example.pomodoro.service.PomodoroService;
import org.example.pomodoro.service.NotificationService;
import org.example.pomodoro.service.SoundService;
import org.example.pomodoro.service.TimerService;
import org.example.pomodoro.service.ThemeService;
import org.example.pomodoro.service.TimerSettingsStore;
import org.example.pomodoro.sync.SyncService;
import org.example.pomodoro.sync.SupabaseApi;

import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class PomodoroController {

    private static final Logger LOGGER =
            Logger.getLogger(PomodoroController.class.getName());
    private static final String LIGHT_THEME_STYLESHEET =
            PomodoroController.class
                    .getResource("/css/light-theme.css")
                    .toExternalForm();

    @FXML
    private Label modeLabel;
    @FXML
    private Label timerLabel;
    @FXML
    private Label cycleLabel;
    @FXML
    private Label settingStatusLabel;
    @FXML
    private Spinner<Integer> focusMinutesSpinner;
    @FXML
    private Spinner<Integer> shortBreakMinutesSpinner;
    @FXML
    private Spinner<Integer> longBreakMinutesSpinner;
    @FXML
    private Spinner<Integer> cycleSpinner;
    @FXML
    private HistoryController historyViewController;
    @FXML
    private StackPane appRoot;
    @FXML
    private ComboBox<AppTheme> themeComboBox;
    @FXML
    private ToggleButton settingsToggleButton;
    @FXML
    private VBox settingsPanel;
    @FXML
    private Button signInButton;
    @FXML
    private Button syncButton;
    @FXML
    private Label syncStatusLabel;

    private Instant sessionStartedAt;

    private final SoundService soundService;
    private final NotificationService notificationService;
    private final PomodoroService pomodoroService;
    private final TimerService timerService;
    private final FocusHistoryService focusHistoryService;
    private final ThemeService themeService;
    private final TimerSettingsStore timerSettingsStore;
    private final SyncService syncService;
    private final ExecutorService syncExecutor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "pomodoro-sync");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean syncBusy = new AtomicBoolean();
    private boolean applyingRemoteSettings;

    public PomodoroController(
            PomodoroService pomodoroService,
            TimerService timerService,
            FocusHistoryService focusHistoryService,
            SoundService soundService,
            NotificationService notificationService,
            ThemeService themeService,
            TimerSettingsStore timerSettingsStore,
            SyncService syncService
    ) {
        this.pomodoroService = pomodoroService;
        this.timerService = timerService;
        this.focusHistoryService = focusHistoryService;
        this.soundService = soundService;
        this.notificationService = notificationService;
        this.themeService = themeService;
        this.timerSettingsStore = timerSettingsStore;
        this.syncService = syncService;
    }

    @FXML
    public void initialize() {
        initializeTheme();
        initializeSettings();
        updateCurrentMode();
        resetTimeDisplay();
        boolean configured = syncService != null;
        signInButton.setDisable(!configured);
        syncButton.setDisable(!configured);
        syncStatusLabel.setText(configured ? "Sign in to sync" : "Local only");
    }

    public void startAutoSync() {
        if (syncService == null) {
            return;
        }
        try {
            if (syncService.hasSavedLogin()) {
                runSync(syncService::sync, false, "Automatic sync");
            }
        } catch (RuntimeException exception) {
            syncStatusLabel.setText("Saved login unavailable; sign in again");
            LOGGER.log(Level.WARNING, "Could not read saved Supabase login", exception);
        }
    }

    public void syncOnClose() {
        if (syncService == null) {
            syncExecutor.shutdownNow();
            return;
        }
        try {
            if (syncService.hasSavedLogin()) {
                Future<?> attempt = syncExecutor.submit(syncService::uploadOnly);
                attempt.get(5, TimeUnit.SECONDS);
            }
        } catch (Exception exception) {
            LOGGER.log(Level.INFO,
                    "Sync on close did not finish; data remains local: " + exception.getMessage());
        } finally {
            syncExecutor.shutdownNow();
        }
    }

    @FXML
    private void handleSyncNow() {
        runSync(syncService::sync, true, "Supabase sync");
    }

    @FXML
    private void handleSyncSignIn() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Supabase sign in");
        dialog.setHeaderText("Use the same account on each computer");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextField email = new TextField();
        email.setPromptText("Email");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        GridPane fields = new GridPane();
        fields.setHgap(10);
        fields.setVgap(10);
        fields.add(new Label("Email"), 0, 0);
        fields.add(email, 1, 0);
        fields.add(new Label("Password"), 0, 1);
        fields.add(password, 1, 1);
        dialog.getDialogPane().setContent(fields);
        if (dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK
                && !email.getText().isBlank() && !password.getText().isBlank()) {
            String emailValue = email.getText().trim();
            String passwordValue = password.getText();
            runSync(() -> syncService.signIn(emailValue, passwordValue),
                    true, "Supabase sign-in");
        }
    }

    private void runSync(Runnable action, boolean showAlert, String title) {
        if (!syncBusy.compareAndSet(false, true)) {
            return;
        }
        syncStatusLabel.setText("Syncing...");
        syncButton.setDisable(true);
        signInButton.setDisable(true);
        syncExecutor.submit(() -> {
            try {
                action.run();
                Platform.runLater(() -> {
                    try {
                        reloadFromLocalDatabase();
                        syncStatusLabel.setText("Synced");
                    } catch (RuntimeException exception) {
                        showSyncError(exception, showAlert, title);
                    }
                });
            } catch (RuntimeException exception) {
                if (exception instanceof SupabaseApi.SyncException) {
                    LOGGER.log(Level.INFO, "Supabase sync failed: {0}",
                            exception.getMessage());
                } else {
                    LOGGER.log(Level.WARNING, "Supabase sync failed", exception);
                }
                Platform.runLater(() -> showSyncError(exception, showAlert, title));
            } finally {
                syncBusy.set(false);
                Platform.runLater(() -> {
                    syncButton.setDisable(false);
                    signInButton.setDisable(false);
                });
            }
        });
    }

    private void showSyncError(RuntimeException exception, boolean showAlert,
                               String title) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = "Unexpected sync error. Local data has been kept.";
        }
        syncStatusLabel.setText(message);
        if (showAlert) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(title + " failed");
            alert.setContentText(message);
            alert.show();
        }
    }

    private void reloadFromLocalDatabase() {
        applyingRemoteSettings = true;
        try {
            AppTheme theme = themeService.getTheme();
            themeComboBox.setValue(theme);
            applyTheme(theme);
        } finally {
            applyingRemoteSettings = false;
        }
        if (!timerService.isRunning() && !timerService.isPaused()) {
            timerSettingsStore.loadInto(pomodoroService);
            focusMinutesSpinner.getValueFactory().setValue(pomodoroService.getFocusMinutes());
            shortBreakMinutesSpinner.getValueFactory().setValue(pomodoroService.getShortBreakMinutes());
            longBreakMinutesSpinner.getValueFactory().setValue(pomodoroService.getLongBreakMinutes());
            cycleSpinner.getValueFactory().setValue(pomodoroService.getCycleBeforeLongBreak());
            updateCurrentMode();
            resetTimeDisplay();
        }
        if (historyViewController != null) {
            historyViewController.reloadSyncedSettings();
        }
    }

    private void initializeTheme() {
        themeComboBox.setItems(
                FXCollections.observableArrayList(AppTheme.values())
        );
        AppTheme savedTheme;

        try {
            savedTheme = themeService.getTheme();
        } catch (RuntimeException exception) {
            savedTheme = AppTheme.DARK;
            LOGGER.log(Level.WARNING, "Could not load theme preference", exception);
        }

        applyingRemoteSettings = true;
        try {
            themeComboBox.setValue(savedTheme);
            applyTheme(savedTheme);
        } finally {
            applyingRemoteSettings = false;
        }
    }

    @FXML
    private void handleThemeChanged() {
        AppTheme selectedTheme = themeComboBox.getValue();

        if (selectedTheme == null) {
            return;
        }

        applyTheme(selectedTheme);

        if (applyingRemoteSettings) {
            return;
        }

        try {
            themeService.updateTheme(selectedTheme);
        } catch (RuntimeException exception) {
            LOGGER.log(Level.WARNING, "Could not save theme preference", exception);
        }
    }

    private void applyTheme(AppTheme theme) {
        appRoot.getStyleClass().removeAll("theme-dark", "theme-light");
        appRoot.getStyleClass().add(theme.getStyleClass());
        appRoot.getStylesheets().remove(LIGHT_THEME_STYLESHEET);

        if (theme == AppTheme.LIGHT) {
            appRoot.getStylesheets().add(LIGHT_THEME_STYLESHEET);
        }
    }

    private void initializeSettings() {
        configureSpinner(
                focusMinutesSpinner,
                1,
                180,
                pomodoroService.getFocusMinutes()
        );
        configureSpinner(
                shortBreakMinutesSpinner,
                1,
                60,
                pomodoroService.getShortBreakMinutes()
        );
        configureSpinner(
                longBreakMinutesSpinner,
                1,
                120,
                pomodoroService.getLongBreakMinutes()
        );
        configureSpinner(
                cycleSpinner,
                1,
                12,
                pomodoroService.getCycleBeforeLongBreak()
        );
    }

    private void configureSpinner(
            Spinner<Integer> spinner,
            int minimum,
            int maximum,
            int initialValue
    ) {
        spinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        minimum,
                        maximum,
                        initialValue
                )
        );
        spinner.setEditable(true);
    }

    @FXML
    private void handleToggleSettings() {
        setSettingsVisible(settingsToggleButton.isSelected());
    }

    private void setSettingsVisible(boolean visible) {
        settingsPanel.setVisible(visible);
        settingsPanel.setManaged(visible);
        settingsToggleButton.setSelected(visible);
    }

    @FXML
    private void handleStart() {
        if (timerService.isRunning()) {
            return;
        }

        if (timerService.isPaused()) {
            timerService.resume();
            return;
        }

        startCurrentTimer();
    }

    private void startCurrentTimer() {
        if (pomodoroService.getCurrentMode() != PomodoroMode.LONG_BREAK) {
            sessionStartedAt = Instant.now();
        }

        timerService.start(
                pomodoroService.getCurrentDurationSeconds(),
                this::updateTimer,
                this::onTimerFinished
        );
    }

    @FXML
    private void handlePause() {
        timerService.pause();
    }

    @FXML
    private void handleReset() {
        timerService.stop();
        sessionStartedAt = null;
        pomodoroService.reset();
        updateCurrentMode();
        resetTimeDisplay();
    }

    @FXML
    private void handleSkip() {
        timerService.stop();
        sessionStartedAt = null;
        pomodoroService.moveToNextMode();
        updateCurrentMode();
        resetTimeDisplay();
    }

    @FXML
    private void handleSaveSettings() {
        try {
            int focusMinutes = readSpinnerValue(
                    focusMinutesSpinner,
                    1,
                    180
            );
            int shortBreakMinutes = readSpinnerValue(
                    shortBreakMinutesSpinner,
                    1,
                    60
            );
            int longBreakMinutes = readSpinnerValue(
                    longBreakMinutesSpinner,
                    1,
                    120
            );
            int cycles = readSpinnerValue(cycleSpinner, 1, 12);

            pomodoroService.updateSettings(
                    focusMinutes,
                    shortBreakMinutes,
                    longBreakMinutes,
                    cycles
            );
            timerSettingsStore.save(pomodoroService);

            handleReset();
            settingStatusLabel.setText("Settings saved. Timer reset.");
            setSettingsVisible(false);
        } catch (IllegalArgumentException exception) {
            settingStatusLabel.setText(
                    "Please enter valid values."
            );
        }
    }

    private int readSpinnerValue(
            Spinner<Integer> spinner,
            int minimum,
            int maximum
    ) {
        int value = Integer.parseInt(spinner.getEditor().getText().trim());

        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException("Value is out of range");
        }

        spinner.getValueFactory().setValue(value);
        return value;
    }

    private void onTimerFinished() {
        PomodoroMode finishedMode = pomodoroService.getCurrentMode();

        if (finishedMode != PomodoroMode.LONG_BREAK) {
            saveCompletedSession(finishedMode);
            sessionStartedAt = null;
        }

        if (finishedMode == PomodoroMode.FOCUS) {
            soundService.playFocusFinished();
        } else {
            soundService.playBreakFinished();
        }

        pomodoroService.moveToNextMode();
        updateCurrentMode();
        resetTimeDisplay();

        notificationService.showSessionFinished(
                appRoot.getScene().getWindow(),
                finishedMode,
                pomodoroService.getCurrentMode(),
                pomodoroService.getCurrentDurationSeconds(),
                themeComboBox.getValue()
        );

        if (finishedMode == PomodoroMode.FOCUS) {
            startCurrentTimer();
        }
    }

    private void saveCompletedSession(PomodoroMode finishedMode) {
        try {
            focusHistoryService.save(
                    sessionStartedAt,
                    Instant.now(),
                    pomodoroService.getCurrentDurationSeconds(),
                    finishedMode
            );

            if (historyViewController != null) {
                historyViewController.refresh();
            }
        } catch (RuntimeException exception) {
            LOGGER.log(
                    Level.SEVERE,
                    "Could not save completed Pomodoro session",
                    exception
            );
        }
    }

    private void resetTimeDisplay() {
        updateTimer(pomodoroService.getCurrentDurationSeconds());
    }

    private void updateTimer(int seconds) {
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        timerLabel.setText(
                "%02d:%02d".formatted(minutes, remainingSeconds)
        );
    }

    private void updateCurrentMode() {
        PomodoroMode mode = pomodoroService.getCurrentMode();

        String text = switch (mode) {
            case FOCUS -> "Focus";
            case LONG_BREAK -> "Long Break";
            case SHORT_BREAK -> "Short Break";
        };

        modeLabel.setText(text);
        modeLabel.getStyleClass().removeAll(
                "focus-mode",
                "short-break-mode",
                "long-break-mode"
        );
        modeLabel.getStyleClass().add(
                switch (mode) {
                    case FOCUS -> "focus-mode";
                    case SHORT_BREAK -> "short-break-mode";
                    case LONG_BREAK -> "long-break-mode";
                }
        );
        updateCycle();
    }

    private void updateCycle() {
        if (pomodoroService.getCurrentMode() == PomodoroMode.LONG_BREAK) {
            cycleLabel.setText("Cycle completed");
            return;
        }

        cycleLabel.setText(
                "Session %d / %d".formatted(
                        pomodoroService.getCurrentFocusNumber(),
                        pomodoroService.getCycleBeforeLongBreak()
                )
        );
    }
}
