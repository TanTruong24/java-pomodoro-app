package org.example.pomodoro;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.pomodoro.config.AppContext;
import org.example.pomodoro.controller.PomodoroController;

import java.io.IOException;
import java.net.URL;

public class MainApplication extends Application {

    private AppContext appContext;

    @Override
    public void start(Stage stage) throws IOException {

        appContext = new AppContext();

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/view/pomodoro-view.fxml")
        );

        loader.setControllerFactory(controllerClass -> {

            if (controllerClass == PomodoroController.class) {
                return appContext.createPomodoroController();
            }

            throw new IllegalArgumentException(
                    "Unknown controller: " + controllerClass
            );
        });

        Scene scene = new Scene(loader.load());

        stage.setTitle("Pomodoro");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
