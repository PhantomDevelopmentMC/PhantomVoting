package me.fergs.phantomvoting.database;

import me.fergs.phantomvoting.database.interfaces.VoteStorage;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

/*
 * TODO: Complete the implementation of the AbstractVoteStorage class.
 */
public abstract class AbstractVoteStorage implements VoteStorage {
    protected Connection connection;
    protected final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AbstractVoteStorage() {
        try {
            connectDatabase();
            initializeDatabase();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
