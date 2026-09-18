package org.example.pomodoro.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.example.pomodoro.model.PomodoroMode;
import org.example.pomodoro.service.PomodoroService;
import org.example.pomodoro.service.TimerService;

public class PomodoroController {

    @FXML
    private Label modeLabel;

    @FXML
    private Label timerLabel;

    @FXML
    private Label cycleLabel;

    private final PomodoroService pomodoroService;
    private final TimerService timerService;

    public PomodoroController(PomodoroService pomodoroService, TimerService timerService) {
        this.pomodoroService = pomodoroService;
        this.timerService = timerService;
    }

    @FXML
    public void initialize(){
        updateCurrentMode();
        resetTimeDisplay();
    }

    @FXML
    private void handleStart(){

        if (timerService.isRunning()) return;

        if (timerService.isPaused()){
            timerService.resume();
            return;
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
        pomodoroService.reset();
        updateCurrentMode();
        resetTimeDisplay();
    }

    @FXML
    private void handleSkip(){

        timerService.stop();
        pomodoroService.moveToNextMode();
        updateCurrentMode();
        resetTimeDisplay();
    }

    private void onTimerFinished(){
        pomodoroService.moveToNextMode();
        updateCurrentMode();
        resetTimeDisplay();
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
