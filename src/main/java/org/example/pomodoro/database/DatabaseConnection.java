package org.example.pomodoro.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    public Connection getConnection() throws SQLException{
        return DriverManager.getConnection(DatabaseConfig.getUrl());
    }
}
