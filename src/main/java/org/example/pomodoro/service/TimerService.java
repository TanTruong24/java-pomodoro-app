package org.example.pomodoro.service;

import java.util.function.IntConsumer;

public interface TimerService {

    void start(
            int durationSeconds,
            IntConsumer onTick,
            Runnable onFinished
    );

    void pause();

    void resume();

    void stop();

    boolean isRunning();

    boolean isPaused();

    int getRemainingSeconds();
}
