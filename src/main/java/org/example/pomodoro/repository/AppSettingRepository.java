package org.example.pomodoro.repository;

import java.util.Optional;

public interface AppSettingRepository {

    Optional<String> find(String key);

    void save(String key, String value);
}
