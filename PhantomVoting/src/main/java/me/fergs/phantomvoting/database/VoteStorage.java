package me.fergs.phantomvoting.database;

import me.fergs.phantomvoting.objects.PlayerVoteData;
import me.fergs.phantomvoting.utils.ConsoleUtil;
import org.bukkit.Bukkit;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class VoteStorage {
    private Connection connection;
    private final TreeSet<PlayerVoteData> cachedTopPlayers = new TreeSet<>(
            Comparator.comparingInt(PlayerVoteData::getVoteCount).reversed()
                    .thenComparing(PlayerVoteData::getUuid)
    );
    private final Map<UUID, Set<Integer>> milestoneCache = new ConcurrentHashMap<>();
    private final Map<UUID, Set<Integer>> streakCache = new ConcurrentHashMap<>();
    /**
     * Creates a new VoteStorage instance.
     *
     * @param dataFolder The plugin's data folder.
     */
    public VoteStorage(String dataFolder) {
        try {
            connectDatabase(dataFolder);
            initializeDatabase();
            Bukkit.getLogger().info(ConsoleUtil.translateColors("&6[&e!&6] &eConnected to &fSQLite &edatabase."));
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
                "yearly_timestamp TEXT," +
                "streak_count INTEGER DEFAULT 0," +
                "last_vote_date TEXT" +
                ");";

        String createVotePartyTableSQL = "CREATE TABLE IF NOT EXISTS vote_party (" +
                "current_vote_count INTEGER DEFAULT 0);";

        String createMilestonesTableSQL = "CREATE TABLE IF NOT EXISTS player_milestones (" +
                "uuid TEXT NOT NULL," +
                "milestone_id INTEGER NOT NULL," +
                "claimed BOOLEAN DEFAULT FALSE," +
                "PRIMARY KEY (uuid, milestone_id)" +
                ");";

        String createStreaksTableSQL = "CREATE TABLE IF NOT EXISTS player_streaks (" +
                "uuid TEXT NOT NULL," +
                "streak_id INTEGER NOT NULL," +
                "claimed BOOLEAN DEFAULT FALSE," +
                "PRIMARY KEY (uuid, streak_id)" +
                ");";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);
            stmt.execute(createVotePartyTableSQL);
            stmt.execute(createMilestonesTableSQL);
            stmt.execute(createStreaksTableSQL);
        }

        checkAndAddColumns();
    }
    /**
     * Checks if the necessary columns exist in the database and adds them if they
     * are missing.
     */
    public void checkAndAddColumns() {
        try {
            String addStreakCountColumnQuery = "ALTER TABLE player_votes ADD COLUMN streak_count INTEGER DEFAULT 0";
            String addLastVoteDateColumnQuery = "ALTER TABLE player_votes ADD COLUMN last_vote_date TEXT";
            try (PreparedStatement stmt = connection.prepareStatement(addStreakCountColumnQuery)) {
                stmt.executeUpdate();
            } catch (SQLException e) {
                if (!e.getMessage().contains("duplicate column name")) {
                    throw e;
                }
            }

            try (PreparedStatement stmt = connection.prepareStatement(addLastVoteDateColumnQuery)) {
                stmt.executeUpdate();
            } catch (SQLException e) {
                if (!e.getMessage().contains("duplicate column name")) {
                    throw e;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
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
                    addVoteStreak(playerUUID);
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

        cachedTopPlayers.clear();
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

        updateTopPlayers(topPlayers);
        return topPlayers;
    }
    /**
     * Updates the cached top players list.
     * @param newTopPlayers The new top players list
     */
    private void updateTopPlayers(List<PlayerVoteData> newTopPlayers) {
        cachedTopPlayers.clear();
        cachedTopPlayers.addAll(
                newTopPlayers.stream()
                        .sorted(Comparator.comparingInt(PlayerVoteData::getVoteCount).reversed()
                                .thenComparing(PlayerVoteData::getUuid))
                        .limit(10)
                        .collect(Collectors.toList())
        );
    }
    /**
     * Gets the PlayerVoteData at the specified position (1-based index).
     *
     * @param position The position (1-based index).
     * @return The PlayerVoteData at the position, or null if out of bounds.
     */
    public PlayerVoteData getTopPlayerAt(int position) {
        if (position <= 0 || position > cachedTopPlayers.size()) {
            return null;
        }
        int currentIndex = 1;
        for (PlayerVoteData data : cachedTopPlayers) {
            if (currentIndex == position) {
                return data;
            }
            currentIndex++;
        }
        return null;
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
     * Adds a vote streak for the specified player, updates streaks based on last vote date,
     * and resets if they missed a day.
     *
     * @param playerUUID UUID of the player
     */
    public void addVoteStreak(UUID playerUUID) {
        LocalDate today = LocalDate.now();
        String currentTimestamp = today.toString();

        try {
            String selectSQL = "SELECT * FROM player_votes WHERE uuid = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(selectSQL)) {
                pstmt.setString(1, playerUUID.toString());
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    if (rs.getString("last_vote_date") == null || rs.getString("streak_count") == null) {
                        incrementStreak(playerUUID, 1, currentTimestamp);
                        return;
                    }
                    LocalDate lastVoteDate = LocalDate.parse(rs.getString("last_vote_date"));
                    int currentStreak = rs.getInt("streak_count");

                    if (lastVoteDate.equals(today.minusDays(1))) {
                        incrementStreak(playerUUID, currentStreak + 1, currentTimestamp);
                    } else if (lastVoteDate.isBefore(today.minusDays(1))) {
                        resetStreak(playerUUID, currentTimestamp);
                    } else {
                        updateVoteDate(playerUUID, currentTimestamp);
                    }
                } else {
                    String insertSQL = "INSERT INTO player_votes(uuid, daily_count, weekly_count, monthly_count, yearly_count, " +
                            "all_time_count, streak_count, last_vote_date, daily_timestamp, weekly_timestamp, monthly_timestamp, yearly_timestamp) " +
                            "VALUES (?, 1, 1, 1, 1, 1, 1, ?, ?, ?, ?, ?)";
                    try (PreparedStatement insertStmt = connection.prepareStatement(insertSQL)) {
                        insertStmt.setString(1, playerUUID.toString());
                        insertStmt.setString(2, currentTimestamp);
                        insertStmt.setString(3, currentTimestamp);
                        insertStmt.setString(4, currentTimestamp);
                        insertStmt.setString(5, currentTimestamp);
                        insertStmt.setString(6, currentTimestamp);
                        insertStmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    /**
     * Sets the vote streak count for the player.
     *
     * @param playerUUID UUID of the player
     * @param streak The streak count to set
     */
    public void setVoteStreak(UUID playerUUID, int streak) {
        String updateSQL = "UPDATE player_votes SET streak_count = ? WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(updateSQL)) {
            pstmt.setInt(1, streak);
            pstmt.setString(2, playerUUID.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    /**
     * Increments the streak count for the player and updates the last vote date.
     *
     * @param playerUUID UUID of the player
     * @param newStreakCount New streak count to set
     * @param currentTimestamp The current date as a timestamp
     */
    public void incrementStreak(UUID playerUUID, int newStreakCount, String currentTimestamp) {
        String updateSQL = "UPDATE player_votes SET streak_count = ?, last_vote_date = ? WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(updateSQL)) {
            pstmt.setInt(1, newStreakCount);
            pstmt.setString(2, currentTimestamp);
            pstmt.setString(3, playerUUID.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    /**
     * Adds to the player's current streak count.
     *
     * @param playerUUID UUID of the player
     * @param streak The streak count to add
     */
    public void addStreak(UUID playerUUID, int streak) {
        String updateSQL = "UPDATE player_votes SET streak_count = streak_count + ? WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(updateSQL)) {
            pstmt.setInt(1, streak);
            pstmt.setString(2, playerUUID.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    /**
     * Resets the player's streak count and updates the last vote date.
     *
     * @param playerUUID UUID of the player
     * @param currentTimestamp The current date as a timestamp
     */
    public void resetStreak(UUID playerUUID, String currentTimestamp) {
        String updateSQL = "UPDATE player_votes SET streak_count = 1, last_vote_date = ? WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(updateSQL)) {
            pstmt.setString(1, currentTimestamp);
            pstmt.setString(2, playerUUID.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    /**
     * Updates only the last vote date without changing the streak count.
     *
     * @param playerUUID UUID of the player
     * @param currentTimestamp The current date as a timestamp
     */
    private void updateVoteDate(UUID playerUUID, String currentTimestamp) {
        String updateSQL = "UPDATE player_votes SET last_vote_date = ? WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(updateSQL)) {
            pstmt.setString(1, currentTimestamp);
            pstmt.setString(2, playerUUID.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    /**
     * Gets the current streak count for the player.
     *
     * @param playerUUID UUID of the player
     * @return The player's current streak count
     */
    public int getPlayerStreak(UUID playerUUID) {
        String querySQL = "SELECT streak_count FROM player_votes WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(querySQL)) {
            pstmt.setString(1, playerUUID.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("streak_count");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    /**
     * Adds a milestone claim for the specified player.
     *
     * @param uuid UUID of the player
     * @param milestoneId ID of the milestone
     */
    public boolean isMilestoneClaimed(UUID uuid, int milestoneId) {
        return milestoneCache.getOrDefault(uuid, Collections.emptySet()).contains(milestoneId);
    }
    /**
     * Claims a milestone for the specified player.
     *
     * @param uuid UUID of the player
     * @param milestoneId ID of the milestone
     */
    public void claimMilestone(UUID uuid, int milestoneId) throws SQLException {
        milestoneCache.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet()).add(milestoneId);

        CompletableFuture.runAsync(() -> {
            String query = "INSERT INTO player_milestones (uuid, milestone_id, claimed) " +
                    "VALUES (?, ?, TRUE) ON CONFLICT(uuid, milestone_id) DO UPDATE SET claimed = TRUE;";
            try (PreparedStatement ps = connection.prepareStatement(query)) {
                ps.setString(1, uuid.toString());
                ps.setInt(2, milestoneId);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
    /**
     * Saves the player milestones to the database.
     */
    public void saveMilestones() throws SQLException {
        String query = "INSERT INTO player_milestones (uuid, milestone_id, claimed) " +
                "VALUES (?, ?, TRUE) ON CONFLICT(uuid, milestone_id) DO UPDATE SET claimed = TRUE;";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            for (Map.Entry<UUID, Set<Integer>> entry : milestoneCache.entrySet()) {
                UUID uuid = entry.getKey();
                for (int milestoneId : entry.getValue()) {
                    ps.setString(1, uuid.toString());
                    ps.setInt(2, milestoneId);
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }

        Bukkit.getLogger().info(ConsoleUtil.translateColors("&6[&e!&6] &eSaved &6" + milestoneCache.size() + " &eplayer milestones to the database."));
    }
    /**
     * Loads the player milestones from the database.
     */
    public void loadMilestones() throws SQLException {
        String query = "SELECT uuid, milestone_id FROM player_milestones WHERE claimed = TRUE;";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                int milestoneId = rs.getInt("milestone_id");
                milestoneCache.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet()).add(milestoneId);
            }
        }

        Bukkit.getLogger().info(ConsoleUtil.translateColors("&6[&e!&6] &eLoaded &6" + milestoneCache.size() + " &eplayer milestones from the database."));
    }
    /**
     * Saves the player streaks to the database.
     */
    public void saveStreaks() throws SQLException {
        String query = "INSERT INTO player_streaks (uuid, streak_id, claimed) " +
                "VALUES (?, ?, TRUE) ON CONFLICT(uuid, streak_id) DO UPDATE SET claimed = TRUE;";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            for (Map.Entry<UUID, Set<Integer>> entry : streakCache.entrySet()) {
                UUID uuid = entry.getKey();
                for (int streakId : entry.getValue()) {
                    ps.setString(1, uuid.toString());
                    ps.setInt(2, streakId);
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }

        Bukkit.getLogger().info(ConsoleUtil.translateColors("&6[&e!&6] &eSaved &6" + streakCache.size() + " &eplayer streaks to the database."));
    }
    /**
     * Loads the player streaks from the database.
     */
    public void loadStreaks() throws SQLException {
        String query = "SELECT uuid, streak_id FROM player_streaks WHERE claimed = TRUE;";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                int streakId = rs.getInt("streak_id");
                streakCache.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet()).add(streakId);
            }
        }

        Bukkit.getLogger().info(ConsoleUtil.translateColors("&6[&e!&6] &eLoaded &6" + streakCache.size() + " &eplayer streaks from the database."));
    }
    /**
     * Claims a streak for the specified player.
     *
     * @param uuid UUID of the player
     * @param streakId ID of the streak
     */
    public void claimStreak(UUID uuid, int streakId) {
        streakCache.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet()).add(streakId);

        CompletableFuture.runAsync(() -> {
            String query = "INSERT INTO player_streaks (uuid, streak_id, claimed) " +
                    "VALUES (?, ?, TRUE) ON CONFLICT(uuid, streak_id) DO UPDATE SET claimed = TRUE;";
            try (PreparedStatement ps = connection.prepareStatement(query)) {
                ps.setString(1, uuid.toString());
                ps.setInt(2, streakId);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
    /**
     * Checks if a player has claimed a streak.
     *
     * @param uuid UUID of the player
     * @param streakId ID of the streak
     * @return True if the player has claimed the streak, false otherwise
     */
    public boolean isStreakClaimed(UUID uuid, int streakId) {
        return streakCache.getOrDefault(uuid, Collections.emptySet()).contains(streakId);
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
