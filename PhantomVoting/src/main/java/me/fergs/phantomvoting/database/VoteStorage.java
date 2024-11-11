package me.fergs.phantomvoting.database;

import me.fergs.phantomvoting.objects.PlayerVoteData;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VoteStorage {
    private Connection connection;
    /**
     * Creates a new VoteStorage instance.
     *
     * @param dataFolder The plugin's data folder.
     */
    public VoteStorage(String dataFolder) {
        try {
            connectDatabase(dataFolder);
            initializeDatabase();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    /**
     * Connects to the SQLite database.
     */
    private void connectDatabase(String dataFolder) throws SQLException {
        String url = "jdbc:sqlite:plugins/" + dataFolder + "/votes.db";
        connection = DriverManager.getConnection(url);
    }
    /**
     * Initializes the database by creating the necessary table if it doesn't exist.
     */
    private void initializeDatabase() throws SQLException {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS player_votes (" +
                "uuid TEXT PRIMARY KEY," +
                "daily_count INTEGER DEFAULT 0," +
                "weekly_count INTEGER DEFAULT 0," +
                "monthly_count INTEGER DEFAULT 0," +
                "yearly_count INTEGER DEFAULT 0," +
                "all_time_count INTEGER DEFAULT 0," +
                "daily_timestamp TEXT," +
                "weekly_timestamp TEXT," +
                "monthly_timestamp TEXT," +
                "yearly_timestamp TEXT" +
                ");";
        String createVotePartyTableSQL = "CREATE TABLE IF NOT EXISTS vote_party (" +
                "current_vote_count INTEGER DEFAULT 0);";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);
            stmt.execute(createVotePartyTableSQL);
        }
    }
    /**
     * Adds a vote to the specified player's record and updates the timestamps.
     * If a timestamp is expired, it resets the count and updates the timestamp.
     * @param playerUUID UUID of the player
     */
    public void addVote(UUID playerUUID) {
        String currentTimestamp = LocalDateTime.now().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            String selectSQL = "SELECT * FROM player_votes WHERE uuid = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(selectSQL)) {
                pstmt.setString(1, playerUUID.toString());
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    LocalDateTime dailyTimestamp = LocalDateTime.parse(rs.getString("daily_timestamp"));
                    LocalDateTime weeklyTimestamp = LocalDateTime.parse(rs.getString("weekly_timestamp"));
                    LocalDateTime monthlyTimestamp = LocalDateTime.parse(rs.getString("monthly_timestamp"));
                    LocalDateTime yearlyTimestamp = LocalDateTime.parse(rs.getString("yearly_timestamp"));

                    if (dailyTimestamp.isBefore(now.minusDays(1))) {
                        resetVote("daily", playerUUID, currentTimestamp);
                    } else {
                        incrementVote("daily", playerUUID);
                    }

                    if (weeklyTimestamp.isBefore(now.minusWeeks(1))) {
                        resetVote("weekly", playerUUID, currentTimestamp);
                    } else {
                        incrementVote("weekly", playerUUID);
                    }

                    if (monthlyTimestamp.isBefore(now.minusMonths(1))) {
                        resetVote("monthly", playerUUID, currentTimestamp);
                    } else {
                        incrementVote("monthly", playerUUID);
                    }

                    if (yearlyTimestamp.isBefore(now.minusYears(1))) {
                        resetVote("yearly", playerUUID, currentTimestamp);
                    } else {
                        incrementVote("yearly", playerUUID);
                    }

                    incrementVote("all_time", playerUUID);
                } else {
                    String insertSQL = "INSERT INTO player_votes(uuid, daily_count, weekly_count, monthly_count, yearly_count, all_time_count, " +
                            "daily_timestamp, weekly_timestamp, monthly_timestamp, yearly_timestamp) " +
                            "VALUES (?, 1, 1, 1, 1, 1, ?, ?, ?, ?)";
                    try (PreparedStatement insertStmt = connection.prepareStatement(insertSQL)) {
                        insertStmt.setString(1, playerUUID.toString());
                        insertStmt.setString(2, currentTimestamp);
                        insertStmt.setString(3, currentTimestamp);
                        insertStmt.setString(4, currentTimestamp);
                        insertStmt.setString(5, currentTimestamp);
                        insertStmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Resets a specific vote period if the timestamp is expired (older than the threshold).
     * @param period The vote period to reset (daily, weekly, monthly, yearly)
     * @param playerUUID UUID of the player
     * @param newTimestamp The new timestamp to store
     */
    private void resetVote(String period, UUID playerUUID, String newTimestamp) {
        String updateSQL = "UPDATE player_votes SET " + period + "_count = 0, " + period + "_timestamp = ? WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(updateSQL)) {
            pstmt.setString(1, newTimestamp);
            pstmt.setString(2, playerUUID.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    /**
     * Increments the vote count for a specific period (e.g., all-time votes).
     * @param period The vote period to increment (e.g., all_time)
     * @param playerUUID UUID of the player
     */
    private void incrementVote(String period, UUID playerUUID) {
        String updateSQL = "UPDATE player_votes SET " + period + "_count = " + period + "_count + 1 WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(updateSQL)) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    /**
     * Removes a vote from the specified player's record.
     * @param playerUUID UUID of the player
     */
    public void removeVote(UUID playerUUID, int count) {
        String updateSQL = "UPDATE player_votes SET all_time_count = all_time_count - ? WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(updateSQL)) {
            pstmt.setInt(1, count);
            pstmt.setString(2, playerUUID.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    /**
     * Gets the specified vote count for a player.
     * @param playerUUID UUID of the player
     * @param type Type of vote count to retrieve ("daily", "weekly", "monthly", "yearly", "all_time")
     * @return The vote count
     */
    public int getPlayerVoteCount(UUID playerUUID, String type) {
        String querySQL = "SELECT " + type + "_count FROM player_votes WHERE uuid = ?;";
        try (PreparedStatement pstmt = connection.prepareStatement(querySQL)) {
            pstmt.setString(1, playerUUID.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(type + "_count");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    /**
     * Sets the current global vote count.
     * @param count The current global vote count
     */
    public void setCurrentGlobalVoteCount(int count) {
        String updateSQL = "UPDATE vote_party SET current_vote_count = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(updateSQL)) {
            pstmt.setInt(1, count);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    /**
     * Gets the current global vote count.
     * @return The current global vote count
     */
    public int getCurrentGlobalVoteCount() {
        String querySQL = "SELECT current_vote_count FROM vote_party";
        try (PreparedStatement pstmt = connection.prepareStatement(querySQL)) {
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("current_vote_count");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    /**
     * Gets the top players based on all-time vote count.
     * @return A list of PlayerVoteData objects
     */
    public List<PlayerVoteData> getTopPlayers() {
        String querySQL = "SELECT uuid, all_time_count FROM player_votes ORDER BY all_time_count DESC LIMIT 10";
        List<PlayerVoteData> topPlayers = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(querySQL)) {
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                int count = rs.getInt("all_time_count");
                topPlayers.add(new PlayerVoteData(uuid, count));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return topPlayers;
    }
    /**
     * Gets the position of a player in the all-time vote leaderboard.
     * @param playerId UUID of the player
     * @return The player's position in the leaderboard
     */
    public int getPlayerPosition(UUID playerId) {
        String querySQL = "SELECT COUNT(*) + 1 AS position FROM player_votes WHERE all_time_count > (SELECT all_time_count FROM player_votes WHERE uuid = ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(querySQL)) {
            pstmt.setString(1, playerId.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("position");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    /**
     * Closes the database connection.
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
