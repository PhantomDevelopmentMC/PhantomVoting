package me.fergs.phantomvoting.managers;

import me.fergs.phantomvoting.PhantomVoting;
import me.fergs.phantomvoting.database.VoteStorage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class VotePartyManager {
    private int voteThreshold;
    private int currentVoteCount = 0;
    private final VoteStorage voteStorage;
    private final PhantomVoting plugin;

    /**
     * Creates a new VotePartyManager.
     *
     * @param plugin The main plugin instance.
     */
    public VotePartyManager(PhantomVoting plugin) {
        this.voteThreshold = plugin.getConfigurationManager().getConfig("voteparty").getInt("Settings.Required", 100);
        this.plugin = plugin;
        this.voteStorage = plugin.getVoteStorage();
        loadVoteCount();
    }
    /**
     * Loads the current vote count from storage.
     */
    private void loadVoteCount() {
        this.currentVoteCount = voteStorage.getCurrentGlobalVoteCount();
    }

    /**
     * Adds a vote to the count and triggers a vote party if the threshold is met.
     */
    public void addVote() {
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
        plugin.getMessageManager().broadcastMessage("VOTE_PARTY_TRIGGERED");

        List<String> partyCommands = plugin.getConfigurationManager()
                .getConfig("voteparty")
                .getStringList("Settings.Commands");

        for (Player player : Bukkit.getOnlinePlayers()) {
            for (String command : partyCommands) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
            }
        }
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
     * Reloads the vote threshold from the configuration.
     */
    public void reloadThreshold() {
        this.voteThreshold = plugin.getConfigurationManager().getConfig("voteparty").getInt("Settings.Required", 100);
    }
}
