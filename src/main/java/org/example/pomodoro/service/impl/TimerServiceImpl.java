package org.example.pomodoro.service.impl;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.util.function.IntConsumer;

public class TimerServiceImpl implements org.example.pomodoro.service.TimerService {

    private Timeline timeline;
    private int remainingSeconds;
    private long deadlineEpochMillis;
    private long pausedRemainingMillis;

    private static final long MILLIS_PER_SECOND = 1_000;

    @Override
    public void start(
            int durationSeconds,
            IntConsumer onTick,
            Runnable onFinished
    ) {
        if (durationSeconds <= 0) {
            throw new IllegalArgumentException(
                    "durationSeconds must be positive"
            );
        }

        stop();
        remainingSeconds = durationSeconds;
        pausedRemainingMillis = Math.multiplyExact(
                (long) durationSeconds,
                MILLIS_PER_SECOND
        );
        deadlineEpochMillis =
                System.currentTimeMillis() + pausedRemainingMillis;

        onTick.accept(remainingSeconds);

        timeline = new Timeline(
                new KeyFrame(
                        Duration.millis(100),
                        event -> {
                            long remainingMillis = Math.max(
                                    0,
                                    deadlineEpochMillis
                                            - System.currentTimeMillis()
                            );

                            int nextRemainingSeconds = (int) Math.ceilDiv(
                                    remainingMillis,
                                    MILLIS_PER_SECOND
                            );

                            if (nextRemainingSeconds != remainingSeconds) {
                                remainingSeconds = nextRemainingSeconds;
                                onTick.accept(remainingSeconds);
                            }

                            if (remainingMillis == 0) {
                                timeline.stop();
                                timeline = null;
                                pausedRemainingMillis = 0;
                                onFinished.run();
                            }
                        }
                )
        );
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    @Override
    public void pause(){
        if (isRunning()) {
            pausedRemainingMillis = Math.max(
                    0,
                    deadlineEpochMillis - System.currentTimeMillis()
            );
            timeline.pause();
        }
    }

    @Override
    public void resume(){
        if (isPaused()) {
            deadlineEpochMillis =
                    System.currentTimeMillis() + pausedRemainingMillis;
            timeline.play();
        }
    }

    @Override
    public void stop(){
        if (timeline != null){
            timeline.stop();
            timeline = null;
        }

        pausedRemainingMillis = 0;
    }

    @Override
    public boolean isRunning(){
        return timeline != null && timeline.getStatus() == Animation.Status.RUNNING;
    }

    @Override
    public boolean isPaused(){
        return timeline != null && timeline.getStatus() == Animation.Status.PAUSED;
    }

    @Override
    public int getRemainingSeconds(){
        return remainingSeconds;
    }
}
