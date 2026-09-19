package org.example.pomodoro.service.impl;

import org.example.pomodoro.model.AppTheme;
import org.example.pomodoro.repository.AppSettingRepository;
import org.example.pomodoro.service.ThemeService;

public class ThemeServiceImpl implements ThemeService {

    private static final String THEME_KEY = "ui_theme";
    private final AppSettingRepository appSettingRepository;

    public ThemeServiceImpl(AppSettingRepository appSettingRepository) {
        this.appSettingRepository = appSettingRepository;
    }

    @Override
    public AppTheme getTheme() {
        return appSettingRepository.find(THEME_KEY)
                .map(this::parseTheme)
                .orElse(AppTheme.DARK);
    }

    @Override
    public void updateTheme(AppTheme theme) {
        appSettingRepository.save(THEME_KEY, theme.name());
    }

    private AppTheme parseTheme(String value) {
        try {
            return AppTheme.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return AppTheme.DARK;
        }
    }
}
