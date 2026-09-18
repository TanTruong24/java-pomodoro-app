package org.example.pomodoro.service.impl;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.util.function.IntConsumer;

public class TimerServiceImpl implements org.example.pomodoro.service.TimerService {

    private Timeline timeline;
    private  int remainingSeconds;

    @Override
    public void start(
            int durationSeconds,
            IntConsumer onTick,
            Runnable onFinished
    ) {
        stop();
        remainingSeconds = durationSeconds;

        onTick.accept(remainingSeconds);

        timeline = new Timeline(
                new KeyFrame(
                        Duration.seconds(1),
                        event -> {
                            remainingSeconds--;
                            onTick.accept(remainingSeconds);

                            if (remainingSeconds <= 0){
                                timeline.stop();
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
        if (timeline != null){
            timeline.pause();
        }
    }

    @Override
    public void resume(){
        if (timeline != null){
            timeline.play();
        }
    }

    @Override
    public void stop(){
        if (timeline != null){
            timeline.stop();
            timeline = null;
        }
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
