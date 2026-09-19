package org.example.pomodoro.service.impl;

import javafx.scene.media.AudioClip;
import org.example.pomodoro.service.SoundService;

import java.util.Objects;

public class SoundServiceImpl implements SoundService {

    private final AudioClip focusFinishedSound;
    private final AudioClip breakFinishedSound;

    public SoundServiceImpl() {

        focusFinishedSound = new AudioClip(
                Objects.requireNonNull(
                        getClass().getResource("/sounds/focus-finished.mp3")
                ).toExternalForm()
        );

        breakFinishedSound = new AudioClip(
                Objects.requireNonNull(
                        getClass().getResource("/sounds/break-finished.mp3")
                ).toExternalForm()
        );
    }

    @Override
    public void playFocusFinished() {
        focusFinishedSound.play();
    }

    @Override
    public void playBreakFinished() {
        breakFinishedSound.play();
    }
}