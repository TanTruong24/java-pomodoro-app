package org.example.pomodoro.model;

public enum AppTheme {
    DARK("Dark", "theme-dark"),
    LIGHT("Light", "theme-light");

    private final String displayName;
    private final String styleClass;

    AppTheme(String displayName, String styleClass) {
        this.displayName = displayName;
        this.styleClass = styleClass;
    }

    public String getStyleClass() {
        return styleClass;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
