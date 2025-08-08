package me.fergs.phantomvoting.database;

import me.fergs.phantomvoting.config.YamlConfigFile;
import me.fergs.phantomvoting.objects.PlayerVoteData;
import me.fergs.phantomvoting.utils.ConsoleUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class VoteStorage {
    private Connection connection;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final TreeSet<PlayerVoteData> cachedTopPlayers = new TreeSet<>(
            Comparator.comparingInt(PlayerVoteData::getVoteCount).reversed()
                    .thenComparing(PlayerVoteData::getUuid)
    );
    private final Map<UUID, Set<Integer>> milestoneCache = new ConcurrentHashMap<>();
    private final Map<UUID, Set<Integer>> streakCache = new ConcurrentHashMap<>();
    private final AtomicInteger currentGlobalVoteCount = new AtomicInteger(0);
    private final String databaseUrl, username, password;
    private final boolean useMySQL;

    /**
     * Creates a new VoteStorage instance.
     *
     * @param dataFolder The plugin's data folder.
     * @param storageConfig The storage configuration.
     */
    public VoteStorage(String dataFolder, YamlConfigFile storageConfig) {
        ConfigurationSection storageSection = storageConfig.getConfigurationSection("Storage-Settings");
        String type = storageSection.getString("type", "SQLITE");
        String host = storageSection.getString("host", "");
        this.username = storageSection.getString("username", "");
        this.password = storageSection.getString("password", "");
        String port = storageSection.getString("port", "");
        String database = storageSection.getString("database", "");
        this.useMySQL = type.equalsIgnoreCase("SQL");
        this.databaseUrl = useMySQL
                ? "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true"
                : "jdbc:sqlite:plugins/" + dataFolder + "/votes.db";

        try {
            connectDatabase();
            initializeDatabase();
            Bukkit.getLogger().info(ConsoleUtil.translateColors("&6[&e!&6] &eConnected to the &f" + (useMySQL ? "MySQL" : "SQLite") + "&e database."));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Connects to the database (SQLite or MySQL).
     */
    private void connectDatabase() throws SQLException {
        if (useMySQL) {
            connection = DriverManager.getConnection(databaseUrl, username, password);
        } else {
            connection = DriverManager.getConnection(databaseUrl);
        }
    }
    private void initializeDatabase() throws SQLException {
        String createTableSQL;
        String createVotePartyTableSQL;
        String createMilestonesTableSQL;
        String createStreaksTableSQL;

        if (useMySQL) {
            createTableSQL = "CREATE TABLE IF NOT EXISTS player_votes (" +
                    "uuid VARCHAR(36) PRIMARY KEY," +
                    "daily_count INT DEFAULT 0," +
                    "weekly_count INT DEFAULT 0," +
                    "monthly_count INT DEFAULT 0," +
                    "yearly_count INT DEFAULT 0," +
                    "all_time_count INT DEFAULT 0," +
                    "daily_timestamp DATETIME," +
                    "weekly_timestamp DATETIME," +
                    "monthly_timestamp DATETIME," +
                    "yearly_timestamp DATETIME," +
                    "streak_count INT DEFAULT 0," +
                    "last_vote_date DATETIME" +
                    ");";

            createVotePartyTableSQL = "CREATE TABLE IF NOT EXISTS vote_party (" +
                    "current_vote_count INT DEFAULT 0" +
                    ");";

            createMilestonesTableSQL = "CREATE TABLE IF NOT EXISTS player_milestones (" +
                    "uuid VARCHAR(36) NOT NULL," +
                    "milestone_id INT NOT NULL," +
                    "claimed BOOLEAN DEFAULT FALSE," +
                    "PRIMARY KEY (uuid, milestone_id)" +
                    ");";

            createStreaksTableSQL = "CREATE TABLE IF NOT EXISTS player_streaks (" +
                    "uuid VARCHAR(36) NOT NULL," +
                    "streak_id INT NOT NULL," +
                    "claimed BOOLEAN DEFAULT FALSE," +
                    "PRIMARY KEY (uuid, streak_id)" +
                    ");";
        } else {
            createTableSQL = "CREATE TABLE IF NOT EXISTS player_votes (" +
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

            createVotePartyTableSQL = "CREATE TABLE IF NOT EXISTS vote_party (" +
                    "current_vote_count INTEGER DEFAULT 0" +
                    ");";

            createMilestonesTableSQL = "CREATE TABLE IF NOT EXISTS player_milestones (" +
                    "uuid TEXT NOT NULL," +
                    "milestone_id INTEGER NOT NULL," +
                    "claimed BOOLEAN DEFAULT FALSE," +
                    "PRIMARY KEY (uuid, milestone_id)" +
                    ");";

            createStreaksTableSQL = "CREATE TABLE IF NOT EXISTS player_streaks (" +
                    "uuid TEXT NOT NULL," +
                    "streak_id INTEGER NOT NULL," +
                    "claimed BOOLEAN DEFAULT FALSE," +
                    "PRIMARY KEY (uuid, streak_id)" +
                    ");";
        }

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(createTableSQL);
            stmt.executeUpdate(createVotePartyTableSQL);
            stmt.executeUpdate(createMilestonesTableSQL);
            stmt.executeUpdate(createStreaksTableSQL);
        }

        checkAndAddColumns();
    }
    /**
     * Checks if the necessary columns exist in the database and adds them if they
     * are missing.
     */
    public void checkAndAddColumns() {
        try {
            if (!columnExists("player_votes", "streak_count")) {
                String addStreakCountColumnQuery = "ALTER TABLE player_votes ADD COLUMN streak_count "
                        + (useMySQL ? "INT DEFAULT 0" : "INTEGER DEFAULT 0");
                try (PreparedStatement stmt = connection.prepareStatement(addStreakCountColumnQuery)) {
                    stmt.executeUpdate();
                }
            }
            if (!columnExists("player_votes", "last_vote_date")) {
                String addLastVoteDateColumnQuery = "ALTER TABLE player_votes ADD COLUMN last_vote_date "
                        + (useMySQL ? "DATETIME" : "TEXT");
                try (PreparedStatement stmt = connection.prepareStatement(addLastVoteDateColumnQuery)) {
                    stmt.executeUpdate();
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
                    LocalDateTime dailyTimestamp;
                    LocalDateTime weeklyTimestamp;
                    LocalDateTime monthlyTimestamp;
                    LocalDateTime yearlyTimestamp;
                    if (useMySQL) {
                        dailyTimestamp = LocalDateTime.parse(rs.getString("daily_timestamp"), formatter);
                        weeklyTimestamp = LocalDateTime.parse(rs.getString("weekly_timestamp"), formatter);
                        monthlyTimestamp = LocalDateTime.parse(rs.getString("monthly_timestamp"), formatter);
                        yearlyTimestamp = LocalDateTime.parse(rs.getString("yearly_timestamp"), formatter);
                    } else {
                        dailyTimestamp = LocalDateTime.parse(rs.getString("daily_timestamp"));
                        weeklyTimestamp = LocalDateTime.parse(rs.getString("weekly_timestamp"));
                        monthlyTimestamp = LocalDateTime.parse(rs.getString("monthly_timestamp"));
                        yearlyTimestamp = LocalDateTime.parse(rs.getString("yearly_timestamp"));
                    }

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
     * Adds multiple votes to the specified player's record and updates all vote counts.
     * If a timestamp is expired, it resets the count and updates the timestamp.
     *
     * @param playerUUID UUID of the player
     * @param voteAmount The number of votes to add
     */
    public void addMultipleVotes(UUID playerUUID, int voteAmount) {
        if (voteAmount <= 0) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        String currentTimestamp = now.toString();

        try {
            String selectSQL = "SELECT * FROM player_votes WHERE uuid = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(selectSQL)) {
                pstmt.setString(1, playerUUID.toString());
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    LocalDateTime dailyTimestamp;
                    LocalDateTime weeklyTimestamp;
                    LocalDateTime monthlyTimestamp;
                    LocalDateTime yearlyTimestamp;
                    if (useMySQL) {
                        dailyTimestamp = LocalDateTime.parse(rs.getString("daily_timestamp"), formatter);
                        weeklyTimestamp = LocalDateTime.parse(rs.getString("weekly_timestamp"), formatter);
                        monthlyTimestamp = LocalDateTime.parse(rs.getString("monthly_timestamp"), formatter);
                        yearlyTimestamp = LocalDateTime.parse(rs.getString("yearly_timestamp"), formatter);
                    } else {
                        dailyTimestamp = LocalDateTime.parse(rs.getString("daily_timestamp"));
                        weeklyTimestamp = LocalDateTime.parse(rs.getString("weekly_timestamp"));
                        monthlyTimestamp = LocalDateTime.parse(rs.getString("monthly_timestamp"));
                        yearlyTimestamp = LocalDateTime.parse(rs.getString("yearly_timestamp"));
                    }

                    boolean resetDaily = dailyTimestamp.isBefore(now.minusDays(1));
                    boolean resetWeekly = weeklyTimestamp.isBefore(now.minusWeeks(1));
                    boolean resetMonthly = monthlyTimestamp.isBefore(now.minusMonths(1));
                    boolean resetYearly = yearlyTimestamp.isBefore(now.minusYears(1));

                    String updateSQL = "UPDATE player_votes SET " +
                            "daily_count = ?, weekly_count = ?, monthly_count = ?, yearly_count = ?, " +
                            "all_time_count = all_time_count + ?," +
                            "daily_timestamp = ?, weekly_timestamp = ?, monthly_timestamp = ?, yearly_timestamp = ? " +
                            "WHERE uuid = ?";

                    try (PreparedStatement updateStmt = connection.prepareStatement(updateSQL)) {
                        updateStmt.setInt(1, resetDaily ? voteAmount : rs.getInt("daily_count") + voteAmount);
                        updateStmt.setInt(2, resetWeekly ? voteAmount : rs.getInt("weekly_count") + voteAmount);
                        updateStmt.setInt(3, resetMonthly ? voteAmount : rs.getInt("monthly_count") + voteAmount);
                        updateStmt.setInt(4, resetYearly ? voteAmount : rs.getInt("yearly_count") + voteAmount);
                        updateStmt.setInt(5, voteAmount);
                        updateStmt.setString(6, resetDaily ? currentTimestamp : rs.getString("daily_timestamp"));
                        updateStmt.setString(7, resetWeekly ? currentTimestamp : rs.getString("weekly_timestamp"));
                        updateStmt.setString(8, resetMonthly ? currentTimestamp : rs.getString("monthly_timestamp"));
                        updateStmt.setString(9, resetYearly ? currentTimestamp : rs.getString("yearly_timestamp"));
                        updateStmt.setString(10, playerUUID.toString());
                        updateStmt.executeUpdate();
                    }
                } else Bukkit.getLogger().info(ConsoleUtil.translateColors("&4[&c!&4] &cPlayer &f" + playerUUID + " &chas no record in the database, please use test-vote to add a record."));
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
        if (count <= 0) {
            count = 0;
        }

        currentGlobalVoteCount.set(count);
    }
    public void saveCurrentGlobalVoteCount() {
        final String updateSQL = "UPDATE vote_party SET current_vote_count = ?;";
        final String insertSQL = "INSERT INTO vote_party (current_vote_count) VALUES (?);";

        try {
            try (PreparedStatement updateStmt = connection.prepareStatement(updateSQL)) {
                updateStmt.setInt(1, currentGlobalVoteCount.get());
                int rows = updateStmt.executeUpdate();
                if (rows > 0) {
                    return;
                }
            }

            try (PreparedStatement insertStmt = connection.prepareStatement(insertSQL)) {
                insertStmt.setInt(1, currentGlobalVoteCount.get());
                insertStmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }    /**
     * Gets the current global vote count.
     * @return The current global vote count
     */
    public int getCurrentGlobalVoteCount() {
        return Math.max(currentGlobalVoteCount.get(), 0);

    }
    /**
     * Loads the current global vote count from the database.
     */
    public void loadCurrentGlobalVoteCount() {
        String querySQL = "SELECT current_vote_count FROM vote_party;";
        try (PreparedStatement pstmt = connection.prepareStatement(querySQL)) {
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                currentGlobalVoteCount.set(rs.getInt("current_vote_count"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
                    LocalDate lastVoteDate;
                    if (useMySQL) {
                        LocalDateTime lastVoteDateTime = LocalDateTime.parse(rs.getString("last_vote_date"), formatter);
                        lastVoteDate = lastVoteDateTime.toLocalDate();
                    } else {
                        lastVoteDate = LocalDate.parse(rs.getString("last_vote_date"));
                    }
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
            String query;
            if (useMySQL) {
                query = "INSERT INTO player_milestones (uuid, milestone_id, claimed) " +
                        "VALUES (?, ?, TRUE) ON DUPLICATE KEY UPDATE claimed = TRUE;";
            }
            else {
                query = "INSERT INTO player_milestones (uuid, milestone_id, claimed) " +
                        "VALUES (?, ?, TRUE) ON CONFLICT(uuid, milestone_id) DO UPDATE SET claimed = TRUE;";
            }
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
        String query;
        if (useMySQL) {
            query = "INSERT INTO player_milestones (uuid, milestone_id, claimed) " +
                    "VALUES (?, ?, TRUE) ON DUPLICATE KEY UPDATE claimed = TRUE;";
        }
        else {
            query = "INSERT INTO player_milestones (uuid, milestone_id, claimed) " +
                    "VALUES (?, ?, TRUE) ON CONFLICT(uuid, milestone_id) DO UPDATE SET claimed = TRUE;";
        }
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
        String query;
        if (useMySQL) {
            query = "INSERT INTO player_streaks (uuid, streak_id, claimed) " +
                    "VALUES (?, ?, TRUE) ON DUPLICATE KEY UPDATE claimed = TRUE;";
        }
        else {
            query = "INSERT INTO player_streaks (uuid, streak_id, claimed) " +
                    "VALUES (?, ?, TRUE) ON CONFLICT(uuid, streak_id) DO UPDATE SET claimed = TRUE;";
        }
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
            String query;
            if (useMySQL) {
                query = "INSERT INTO player_streaks (uuid, streak_id, claimed) " +
                        "VALUES (?, ?, TRUE) ON DUPLICATE KEY UPDATE claimed = TRUE;";
            }
            else {
                query = "INSERT INTO player_streaks (uuid, streak_id, claimed) " +
                        "VALUES (?, ?, TRUE) ON CONFLICT(uuid, streak_id) DO UPDATE SET claimed = TRUE;";
            }
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
     * Checks if a given column exists in the specified table.
     *
     * @param tableName  The table name.
     * @param columnName The column name.
     * @return true if the column exists, false otherwise.
     * @throws SQLException If a database access error occurs.
     */
    private boolean columnExists(String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet rs = metaData.getColumns(null, null, tableName, columnName)) {
            return rs.next();
        }
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
