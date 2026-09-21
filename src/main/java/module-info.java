module org.example.pomodoro {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires javafx.media;
    requires org.xerial.sqlitejdbc;
    requires java.net.http;
    requires jdk.crypto.ec;
    requires com.google.gson;


    opens org.example.pomodoro to javafx.fxml;
    opens org.example.pomodoro.controller to javafx.fxml;
    opens org.example.pomodoro.view to javafx.fxml;
    exports org.example.pomodoro;
}
