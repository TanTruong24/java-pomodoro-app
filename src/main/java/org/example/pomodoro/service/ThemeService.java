package org.example.pomodoro.service;

import org.example.pomodoro.model.AppTheme;

public interface ThemeService {

    AppTheme getTheme();

    void updateTheme(AppTheme theme);
}
