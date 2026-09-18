package org.example.pomodoro.model;

public class PomodoroSetting {

    private int focusMinutes = 25;
    private int shortBreakMinutes = 5;
    private int longBreakMinutes = 15;
    private int cycleBeforeLongBreak = 4;

    public int getFocusMinutes() {
        return focusMinutes;
    }

    public void setFocusMinutes(int focusMinutes) {
        this.focusMinutes = focusMinutes;
    }

    public int getShortBreakMinutes() {
        return shortBreakMinutes;
    }

    public void setShortBreakMinutes(int shortBreakMinutes) {
        this.shortBreakMinutes = shortBreakMinutes;
    }

    public int getLongBreakMinutes() {
        return longBreakMinutes;
    }

    public void setLongBreakMinutes(int longBreakMinutes) {
        this.longBreakMinutes = longBreakMinutes;
    }

    public int getCycleBeforeLongBreak() {
        return cycleBeforeLongBreak;
    }

    public void setCycleBeforeLongBreak(int cycleBeforeLongBreak) {
        this.cycleBeforeLongBreak = cycleBeforeLongBreak;
    }
}
