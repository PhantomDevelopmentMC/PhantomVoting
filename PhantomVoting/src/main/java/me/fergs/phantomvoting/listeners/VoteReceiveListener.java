package me.fergs.phantomvoting.listeners;

import com.vexsoftware.votifier.model.VotifierEvent;
import me.fergs.phantomvoting.PhantomVoting;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
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

        if (PhantomVoting.getInstance().getConfigurationManager().getConfig("config").getBoolean("Sound.enabled")) {
            String soundType = PhantomVoting.getInstance().getConfigurationManager().getConfig("config").getString("Sound.soundType");
            assert soundType != null;
            if (!soundType.isEmpty()) {
                player.playSound(player, Sound.valueOf(soundType), 1.0f, 1.0f);
            }
        }

        ConfigurationSection voteRewardsSection = PhantomVoting.getInstance().getConfigurationManager().getConfig("config").getConfigurationSection("Rewards.VoteRewards");
        if (voteRewardsSection != null) {
            for (String rewardKey : voteRewardsSection.getKeys(false)) {
                ConfigurationSection rewardSection = voteRewardsSection.getConfigurationSection(rewardKey);
                assert rewardSection != null;
                boolean hasPermissionString = rewardSection.contains("Permission");
                double chance = rewardSection.getDouble("Chance", 100);
                if (hasPermissionString && !player.hasPermission(rewardSection.getString("Permission", "phantomvoting.default"))) {
                    continue;
                }
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
