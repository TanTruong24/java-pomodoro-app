package org.example.pomodoro.database;

import org.example.pomodoro.database.schema.Schema;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class DatabaseInitializer {

    private final DatabaseConnection databaseConnection;

    private final List<Schema> schemas;

    public DatabaseInitializer(DatabaseConnection databaseConnection, List<Schema> schemas) {
        this.databaseConnection = databaseConnection;
        this.schemas = schemas;
    }

    public void initialize(){

        try (Connection connection = databaseConnection.getConnection()) {
            for (Schema schema : schemas) {
                schema.create(connection);
            }
        } catch (SQLException e){
            throw new RuntimeException(
                    "Could not initialize database",
                    e
            );
        }
    }
}
