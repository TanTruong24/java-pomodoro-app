package org.example.pomodoro;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.example.pomodoro.config.AppContext;
import org.example.pomodoro.controller.PomodoroController;
import org.example.pomodoro.controller.HistoryController;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;

public class MainApplication extends Application {

    private AppContext appContext;

    @Override
    public void start(Stage stage) throws IOException {

        Locale.setDefault(Locale.ENGLISH);
        appContext = new AppContext();

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/view/pomodoro-view.fxml")
        );

        loader.setControllerFactory(controllerClass -> {

            if (controllerClass == PomodoroController.class) {
                return appContext.createPomodoroController();
            }

            if (controllerClass == HistoryController.class) {
                return appContext.createHistoryController();
            }

            throw new IllegalArgumentException(
                    "Unknown controller: " + controllerClass
            );
        });

        Scene scene = new Scene(loader.load(), 980, 700);

        stage.setTitle("Pomodoro");

        URL iconUrl = getClass().getResource("/icons/app-icon.png");
        if (iconUrl != null) {
            stage.getIcons().add(new Image(iconUrl.toExternalForm()));
        }

        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
