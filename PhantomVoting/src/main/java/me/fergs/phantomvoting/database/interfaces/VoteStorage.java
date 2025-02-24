package me.fergs.phantomvoting.database.interfaces;

import java.sql.SQLException;
import java.util.UUID;

/**
 * TODO: Complete the implementation of the VoteStorage interface.
 */
public interface VoteStorage {
    void connectDatabase() throws SQLException;
    void initializeDatabase() throws SQLException;
    void addVote(UUID playerUUID);
    void closeConnection();
}