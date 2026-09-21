package org.example.pomodoro.service.impl;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Window;
import javafx.util.Duration;
import org.example.pomodoro.model.AppTheme;
import org.example.pomodoro.model.PomodoroMode;
import org.example.pomodoro.service.NotificationService;

import java.util.List;

public class JavaFxNotificationService implements NotificationService {

    private static final double TOAST_WIDTH = 350;
    private static final double SCREEN_MARGIN = 20;
    private static final Duration ENTER_DURATION = Duration.millis(180);
    private static final Duration EXIT_DURATION = Duration.millis(160);
    private static final Duration DISPLAY_DURATION = Duration.seconds(5);

    private static final String BASE_STYLESHEET =
            JavaFxNotificationService.class
                    .getResource("/css/pomodoro.css")
                    .toExternalForm();
    private static final String LIGHT_THEME_STYLESHEET =
            JavaFxNotificationService.class
                    .getResource("/css/light-theme.css")
                    .toExternalForm();

    private Popup activePopup;
    private PauseTransition activeAutoClose;
    private Timeline activeProgress;

    @Override
    public void showSessionFinished(
            Window owner,
            PomodoroMode finishedMode,
            PomodoroMode nextMode,
            int nextDurationSeconds,
            AppTheme theme
    ) {
        if (owner == null || !owner.isShowing()) {
            return;
        }

        closeActivePopup();

        NotificationContent content = createContent(
                finishedMode,
                nextMode,
                nextDurationSeconds
        );
        ProgressBar progressBar = new ProgressBar(1);
        Popup popup = new Popup();
        StackPane toast = createToast(
                content,
                finishedMode,
                progressBar,
                popup
        );

        toast.getStylesheets().add(BASE_STYLESHEET);
        if (theme == AppTheme.LIGHT) {
            toast.getStylesheets().add(LIGHT_THEME_STYLESHEET);
        }

        popup.setAutoFix(true);
        popup.setAutoHide(false);
        popup.setHideOnEscape(false);
        popup.getContent().add(toast);

        Rectangle2D visualBounds = findScreen(owner).getVisualBounds();
        popup.show(
                owner,
                visualBounds.getMaxX() - TOAST_WIDTH - SCREEN_MARGIN,
                visualBounds.getMinY() + SCREEN_MARGIN
        );

        activePopup = popup;
        playEnterAnimation(toast);
        startAutoClose(popup, toast, progressBar);
    }

    private StackPane createToast(
            NotificationContent content,
            PomodoroMode finishedMode,
            ProgressBar progressBar,
            Popup popup
    ) {
        Label iconLabel = new Label(
                finishedMode == PomodoroMode.FOCUS ? "✓" : "→"
        );
        iconLabel.getStyleClass().add("toast-icon-label");

        StackPane icon = new StackPane(iconLabel);
        icon.getStyleClass().add("toast-icon");

        Label title = new Label(content.title());
        title.getStyleClass().add("toast-title");

        Label message = new Label(content.message());
        message.setWrapText(true);
        message.getStyleClass().add("toast-message");

        Label meta = new Label(content.meta());
        meta.getStyleClass().add("toast-meta");

        VBox copy = new VBox(3, title, message, meta);
        copy.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(copy, Priority.ALWAYS);

        Button closeButton = new Button("×");
        closeButton.setId("toastCloseButton");
        closeButton.setFocusTraversable(false);
        closeButton.setAccessibleText("Close notification");
        closeButton.getStyleClass().add("toast-close-button");
        closeButton.setOnAction(event -> hidePopup(popup, toastFor(popup)));

        HBox body = new HBox(12, icon, copy, closeButton);
        body.setAlignment(Pos.TOP_LEFT);
        body.setPadding(new Insets(16, 12, 17, 17));

        Region accent = new Region();
        accent.getStyleClass().add("toast-accent");
        StackPane.setAlignment(accent, Pos.CENTER_LEFT);

        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(3);
        progressBar.setMouseTransparent(true);
        progressBar.getStyleClass().add("toast-progress");
        StackPane.setAlignment(progressBar, Pos.BOTTOM_CENTER);

        StackPane toast = new StackPane(body, accent, progressBar);
        toast.setMinWidth(TOAST_WIDTH);
        toast.setPrefWidth(TOAST_WIDTH);
        toast.setMaxWidth(TOAST_WIDTH);
        toast.getStyleClass().addAll(
                "session-toast",
                finishedMode == PomodoroMode.FOCUS
                        ? "focus-toast"
                        : "break-toast"
        );
        return toast;
    }

    private StackPane toastFor(Popup popup) {
        return (StackPane) popup.getContent().getFirst();
    }

    private void playEnterAnimation(StackPane toast) {
        toast.setOpacity(0);
        toast.setTranslateY(-12);

        FadeTransition fade = new FadeTransition(ENTER_DURATION, toast);
        fade.setToValue(1);

        TranslateTransition slide =
                new TranslateTransition(ENTER_DURATION, toast);
        slide.setToY(0);

        new ParallelTransition(fade, slide).play();
    }

    private void startAutoClose(
            Popup popup,
            StackPane toast,
            ProgressBar progressBar
    ) {
        activeProgress = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(
                        progressBar.progressProperty(),
                        1
                )),
                new KeyFrame(DISPLAY_DURATION, new KeyValue(
                        progressBar.progressProperty(),
                        0
                ))
        );
        activeProgress.play();

        activeAutoClose = new PauseTransition(DISPLAY_DURATION);
        activeAutoClose.setOnFinished(event -> hidePopup(popup, toast));
        activeAutoClose.play();
    }

    private void hidePopup(Popup popup, StackPane toast) {
        if (!popup.isShowing()) {
            return;
        }

        stopTimers();

        FadeTransition fade = new FadeTransition(EXIT_DURATION, toast);
        fade.setToValue(0);

        TranslateTransition slide =
                new TranslateTransition(EXIT_DURATION, toast);
        slide.setToY(-8);

        ParallelTransition exit = new ParallelTransition(fade, slide);
        exit.setOnFinished(event -> {
            popup.hide();
            if (activePopup == popup) {
                activePopup = null;
            }
        });
        exit.play();
    }

    private void closeActivePopup() {
        stopTimers();

        if (activePopup != null) {
            activePopup.hide();
            activePopup = null;
        }
    }

    private void stopTimers() {
        if (activeAutoClose != null) {
            activeAutoClose.stop();
            activeAutoClose = null;
        }

        if (activeProgress != null) {
            activeProgress.stop();
            activeProgress = null;
        }
    }

    private Screen findScreen(Window owner) {
        double centerX = owner.getX() + owner.getWidth() / 2;
        double centerY = owner.getY() + owner.getHeight() / 2;
        List<Screen> matchingScreens =
                Screen.getScreensForRectangle(centerX, centerY, 1, 1);

        return matchingScreens.isEmpty()
                ? Screen.getPrimary()
                : matchingScreens.getFirst();
    }

    private NotificationContent createContent(
            PomodoroMode finishedMode,
            PomodoroMode nextMode,
            int nextDurationSeconds
    ) {
        int nextDurationMinutes = Math.max(1, nextDurationSeconds / 60);
        String nextModeLabel = formatMode(nextMode);

        return switch (finishedMode) {
            case FOCUS -> new NotificationContent(
                    "Focus complete",
                    "Great work! Time for a %d-minute break."
                            .formatted(nextDurationMinutes),
                    "Up next · " + nextModeLabel
            );
            case SHORT_BREAK -> new NotificationContent(
                    "Break complete",
                    "Ready for another %d-minute focus session?"
                            .formatted(nextDurationMinutes),
                    "Up next · " + nextModeLabel
            );
            case LONG_BREAK -> new NotificationContent(
                    "Long break complete",
                    "You're recharged. Let's start a new cycle!",
                    "New cycle · " + nextModeLabel
            );
        };
    }

    private String formatMode(PomodoroMode mode) {
        return switch (mode) {
            case FOCUS -> "Focus";
            case SHORT_BREAK -> "Short Break";
            case LONG_BREAK -> "Long Break";
        };
    }

    private record NotificationContent(
            String title,
            String message,
            String meta
    ) {
    }
}
