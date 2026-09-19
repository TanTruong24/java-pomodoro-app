package org.example.pomodoro.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.example.pomodoro.model.PomodoroMode;
import org.example.pomodoro.service.FocusHistoryService;
import org.example.pomodoro.service.PomodoroService;
import org.example.pomodoro.service.SoundService;
import org.example.pomodoro.service.TimerService;

import java.time.Instant;

public class PomodoroController {

    @FXML
    private Label modeLabel;

    @FXML
    private Label timerLabel;

    @FXML
    private Label cycleLabel;

    private Instant focusStartedAt;

    private final SoundService soundService;

    private final PomodoroService pomodoroService;
    private final TimerService timerService;
    private final FocusHistoryService focusHistoryService;

    public PomodoroController(
            PomodoroService pomodoroService,
            TimerService timerService,
            FocusHistoryService focusHistoryService,
            SoundService soundService
    ) {
        this.pomodoroService = pomodoroService;
        this.timerService = timerService;
        this.focusHistoryService = focusHistoryService;
        this.soundService = soundService;
    }

    @FXML
    public void initialize(){
        updateCurrentMode();
        resetTimeDisplay();
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

        if (pomodoroService.getCurrentMode() == PomodoroMode.FOCUS) {
            focusStartedAt = Instant.now();
        }

        timerService.start(
                pomodoroService.getCurrentDurationSeconds(),
                this::updateTimer,
                this::onTimerFinished
        );
    }

    @FXML
    private void handlePause(){
        timerService.pause();
    }

    @FXML
    private void handleReset(){

        timerService.stop();

        focusStartedAt = null;

        pomodoroService.reset();
        updateCurrentMode();
        resetTimeDisplay();
    }

    @FXML
    private void handleSkip(){

        timerService.stop();

        focusStartedAt = null;
        pomodoroService.moveToNextMode();
        updateCurrentMode();
        resetTimeDisplay();
    }

    private void onTimerFinished() {

        PomodoroMode finishedMode =
                pomodoroService.getCurrentMode();

        // Save completed focus session
        if (finishedMode == PomodoroMode.FOCUS) {

            Instant endedAt = Instant.now();

            focusHistoryService.save(
                    focusStartedAt,
                    endedAt,
                    pomodoroService.getCurrentDurationSeconds()
            );

            focusStartedAt = null;

            soundService.playFocusFinished();
        } else {
            soundService.playBreakFinished();
        }

        // Move: FOCUS -> BREAK
        // or BREAK -> FOCUS
        pomodoroService.moveToNextMode();

        updateCurrentMode();
        resetTimeDisplay();

        // Automatically start break after focus finishes
        if (finishedMode == PomodoroMode.FOCUS) {
            startCurrentTimer();
        }
    }

    private void resetTimeDisplay(){
        updateTimer(pomodoroService.getCurrentDurationSeconds());
    }

    private void updateTimer(int seconds){

        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;

        timerLabel.setText("%02d:%02d".formatted(minutes, remainingSeconds));
    }

    private void updateCurrentMode(){

        PomodoroMode mode = pomodoroService.getCurrentMode();

        String text = switch (mode){
            case FOCUS -> "Focus";
            case LONG_BREAK -> "Long Break";
            case SHORT_BREAK -> "Short Break";
        };

        modeLabel.setText(text);
        updateCycle();
    }

    private void updateCycle() {

        if (pomodoroService.getCurrentMode() == PomodoroMode.LONG_BREAK) {
            cycleLabel.setText("Long break");
            return;
        }

        cycleLabel.setText(
                "Focus %d / %d".formatted(
                        pomodoroService.getCurrentFocusNumber(),
                        pomodoroService.getCycleBeforeLongBreak()
                )
        );
    }


}
