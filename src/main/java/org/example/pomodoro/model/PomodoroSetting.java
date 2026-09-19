package org.example.pomodoro.model;

public class PomodoroSetting {

    private int focusMinutes = 25;
    private int shortBreakMinutes = 5;
    private int longBreakMinutes = 10;
    private int cycleBeforeLongBreak = 4;

    public int getFocusMinutes() {
        return focusMinutes;
    }

    public void setFocusMinutes(int focusMinutes) {
        requirePositive(focusMinutes, "focusMinutes");
        this.focusMinutes = focusMinutes;
    }

    public int getShortBreakMinutes() {
        return shortBreakMinutes;
    }

    public void setShortBreakMinutes(int shortBreakMinutes) {
        requirePositive(shortBreakMinutes, "shortBreakMinutes");
        this.shortBreakMinutes = shortBreakMinutes;
    }

    public int getLongBreakMinutes() {
        return longBreakMinutes;
    }

    public void setLongBreakMinutes(int longBreakMinutes) {
        requirePositive(longBreakMinutes, "longBreakMinutes");
        this.longBreakMinutes = longBreakMinutes;
    }

    public int getCycleBeforeLongBreak() {
        return cycleBeforeLongBreak;
    }

    public void setCycleBeforeLongBreak(int cycleBeforeLongBreak) {
        requirePositive(cycleBeforeLongBreak, "cycleBeforeLongBreak");
        this.cycleBeforeLongBreak = cycleBeforeLongBreak;
    }

    private void requirePositive(int value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(
                    fieldName + " must be positive"
            );
        }
    }
}
