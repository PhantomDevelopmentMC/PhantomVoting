package me.fergs.phantomvoting.managers;

import me.fergs.phantomvoting.PhantomVoting;
import me.fergs.phantomvoting.config.YamlConfigFile;
import me.fergs.phantomvoting.database.VoteStorage;
import me.fergs.phantomvoting.objects.voteparty.ChanceCommandGroup;
import me.fergs.phantomvoting.objects.voteparty.PermissionCommandGroup;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;


public class VotePartyManager {
    private int voteThreshold;
    private int currentVoteCount = 0;
    private final VoteStorage voteStorage;
    private final PhantomVoting plugin;
    private final YamlConfigFile votepartyConfig;
    private boolean votePartyEnabled;

    private final Set<String> cachedCommands = new HashSet<>();
    private final Map<String, PermissionCommandGroup> cachedPermissionCommands = new HashMap<>();
    private final Set<ChanceCommandGroup> cachedChanceCommands = new HashSet<>();

    /**
     * Creates a new VotePartyManager.
     *
     * @param plugin The main plugin instance.
     */
    public VotePartyManager(PhantomVoting plugin) {
        this.votepartyConfig = plugin.getConfigurationManager().getConfig("voteparty");
        this.voteThreshold = votepartyConfig.getInt("Settings.Required", 100);
        this.plugin = plugin;
        this.voteStorage = plugin.getVoteStorage();
        this.votePartyEnabled = votepartyConfig.getBoolean("Settings.Enabled", true);
        loadVoteCount();
        cacheCommands();
    }

    /**
     * Loads the current vote count from storage.
     */
    private void loadVoteCount() {
        this.currentVoteCount = voteStorage.getCurrentGlobalVoteCount();
    }

    /**
     * Caches the commands and command groups to improve performance.
     */
    private void cacheCommands() {
        cachedCommands.clear();
        cachedChanceCommands.clear();
        cachedPermissionCommands.clear();

        cachedCommands.addAll(votepartyConfig.getStringList("Settings.Commands"));

        ConfigurationSection chanceCommandsSection = votepartyConfig.getConfigurationSection("Settings.Chance-Commands");
        if (chanceCommandsSection != null) {
            for (String key : chanceCommandsSection.getKeys(false)) {
                double chance = chanceCommandsSection.getDouble(key + ".Chance", 100.0);
                Set<String> commands = new HashSet<>(chanceCommandsSection.getStringList(key + ".Commands"));
                cachedChanceCommands.add(new ChanceCommandGroup(chance, commands));
            }
        }

        ConfigurationSection permissionCommandsSection = votepartyConfig.getConfigurationSection("Settings.Permission-Commands");
        if (permissionCommandsSection != null) {
            for (String key : permissionCommandsSection.getKeys(false)) {
                String permission = permissionCommandsSection.getString(key + ".Permission", "");
                Set<String> commands = new HashSet<>(permissionCommandsSection.getStringList(key + ".Commands"));
                cachedPermissionCommands.put(key, new PermissionCommandGroup(permission, commands));
            }
        }
    }

    /**
     * Adds a vote to the count and triggers a vote party if the threshold is met.
     */
    public void addVote() {
        if (!votePartyEnabled) {
            return;
        }
        currentVoteCount++;
        voteStorage.setCurrentGlobalVoteCount(currentVoteCount);

        if (currentVoteCount >= voteThreshold) {
            triggerVoteParty();
            resetVoteCount();
        }
    }

    /**
     * Triggers the vote party by executing configured commands for all online players.
     */
    private void triggerVoteParty() {
        if (!votePartyEnabled) {
            return;
        }

        plugin.getMessageManager().broadcastMessage("VOTE_PARTY_TRIGGERED");

        plugin.getPlayerManager().getPlayers().forEach(player -> {
            for (final String command : cachedCommands) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
            }

            cachedChanceCommands.forEach(group -> {
                if (Math.random() * 100 <= group.getChance()) {
                    group.getCommands().forEach(command ->
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()))
                    );
                }
            });

            cachedPermissionCommands.values().forEach(group -> {
                if (player.hasPermission(group.getPermission())) {
                    group.getCommands().forEach(command ->
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()))
                    );
                }
            });
        });
    }

    /**
     * Resets the vote count after a vote party.
     */
    private void resetVoteCount() {
        currentVoteCount = 0;
        voteStorage.setCurrentGlobalVoteCount(currentVoteCount);
    }

    /**
     * Gets the current vote count.
     *
     * @return The current vote count.
     */
    public int getCurrentVoteCount() {
        return currentVoteCount;
    }

    /**
     * Gets the vote threshold.
     *
     * @return The vote threshold.
     */
    public int getVoteThreshold() {
        return voteThreshold;
    }

    /**
     * Reloads the vote threshold and caches from the configuration.
     */
    public void reloadSettings() {
        this.votepartyConfig.reload();
        this.voteThreshold = votepartyConfig.getInt("Settings.Required", 100);
        this.votePartyEnabled = votepartyConfig.getBoolean("Settings.Enabled", true);
        cacheCommands();
    }

    /**
     * Forces a vote party to trigger.
     *
     * @param resetVotes Whether to reset the vote count after triggering.
     */
    public void forceVoteParty(boolean resetVotes) {
        if (resetVotes) {
            resetVoteCount();
        }

        triggerVoteParty();
    }

    /**
     * Forces the vote count to a specific amount.
     *
     * @param amount The amount to set the vote count to.
     */
    public void forceAddAmount(int amount) {
        currentVoteCount += amount;
        voteStorage.setCurrentGlobalVoteCount(currentVoteCount);
    }

    /**
     * Sets the current vote count.
     *
     * @param currentVoteCount The current vote count.
     */
    public void setCurrentVoteCount(int currentVoteCount) {
        this.currentVoteCount = currentVoteCount;
        if (currentVoteCount >= voteThreshold) {
            triggerVoteParty();
            currentVoteCount -= voteThreshold;
        }
        voteStorage.setCurrentGlobalVoteCount(currentVoteCount);
    }
}