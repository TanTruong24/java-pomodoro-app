package org.example.pomodoro.database.schema;

import java.sql.Connection;
import java.sql.SQLException;

public interface Schema {

    void create(Connection connection) throws SQLException;
}
