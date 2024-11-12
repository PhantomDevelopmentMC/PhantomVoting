package me.fergs.phantomvoting.listeners;

import com.vexsoftware.votifier.model.VotifierEvent;
import me.fergs.phantomvoting.PhantomVoting;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.UUID;

/**
 * A listener for handling vote events.
 */
public class VoteReceiveListener implements Listener {
    /**
     * Handles a vote event.
     *
     * @param event the vote event
     */
    @EventHandler
    public void onVoteReceive(VotifierEvent event) {
        String playerName = event.getVote().getUsername();
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) return;

        UUID playerUUID = player.getUniqueId();
        PhantomVoting.getInstance().getVoteStorage().addVote(playerUUID);

        PhantomVoting.getInstance().getMessageManager().broadcastMessage("VOTE_RECEIVED", "%player%", playerName);

        List<String> defaultCommands = PhantomVoting.getInstance().getConfigurationManager().getConfig("config").getStringList("Rewards.Default.Commands");
        for (String command : defaultCommands) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", playerName));
        }

        ConfigurationSection voteRewardsSection = PhantomVoting.getInstance().getConfigurationManager().getConfig("config").getConfigurationSection("Rewards.VoteRewards");
        if (voteRewardsSection != null) {
            for (String rewardKey : voteRewardsSection.getKeys(false)) {
                ConfigurationSection rewardSection = voteRewardsSection.getConfigurationSection(rewardKey);
                assert rewardSection != null;
                double chance = rewardSection.getDouble("Chance", 100);
                if (Math.random() * 100 <= chance) {
                    List<String> rewardCommands = rewardSection.getStringList("Commands");
                    for (String command : rewardCommands) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", playerName));
                    }
                }
            }
        }

        PhantomVoting.getInstance().getVotePartyManager().addVote();
        if (PhantomVoting.getInstance().getConfigurationManager().isModuleEnabled("bossbar")) {
            PhantomVoting.getInstance().getBossbarManager().update();
        }
    }
}
