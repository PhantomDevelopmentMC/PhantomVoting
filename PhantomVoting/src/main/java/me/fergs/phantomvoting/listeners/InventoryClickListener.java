package me.fergs.phantomvoting.listeners;

import me.fergs.phantomvoting.PhantomVoting;
import me.fergs.phantomvoting.inventories.holders.LeaderboardInventoryHolder;
import me.fergs.phantomvoting.inventories.holders.MilestonesInventoryHolder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;

public class InventoryClickListener implements Listener {
    /**
     * Handles the inventory click event.
     *
     * @param event The event.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof LeaderboardInventoryHolder) {
            event.setCancelled(true);
        }

        if (!(event.getInventory().getHolder() instanceof MilestonesInventoryHolder)) return;

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        UUID playerUUID = player.getUniqueId();
        PhantomVoting plugin = PhantomVoting.getInstance();

        ConfigurationSection menuSection = plugin.getConfigurationManager()
                .getConfig("menus/milestones")
                .getConfigurationSection("Milestones.menu");

        if (menuSection == null) return;

        int clickedSlot = event.getSlot();
        if (clickedSlot < 0) return;

        menuSection.getKeys(false).stream()
                .map(menuSection::getConfigurationSection)
                .filter(Objects::nonNull)
                .filter(milestoneConfig -> milestoneConfig.getInt("slot", -1) == clickedSlot)
                .findFirst()
                .ifPresent(milestoneConfig -> {
                    try {
                        int requiredVotes = milestoneConfig.getInt("required-votes");
                        int playerVotes = plugin.getVoteStorage().getPlayerVoteCount(playerUUID, "all_time");
                        int milestoneIndex = Integer.parseInt(milestoneConfig.getName().substring(1));
                        boolean isClaimed = plugin.getVoteStorage().isMilestoneClaimed(playerUUID, milestoneIndex);
                        if (isClaimed) {
                            plugin.getMessageManager().sendMessage(player, "MILESTONE_ALREADY_CLAIMED");
                        } else if (playerVotes < requiredVotes) {
                            plugin.getMessageManager().sendMessage(player, "MILESTONE_NOT_ENOUGH_VOTES", "%required_votes%", String.valueOf(requiredVotes));
                        } else {
                            milestoneConfig.getStringList("Available.commands").forEach(command ->
                                    plugin.getServer().dispatchCommand(
                                            plugin.getServer().getConsoleSender(),
                                            command.replace("%player%", player.getName())
                                    )
                            );

                            plugin.getVoteStorage().claimMilestone(playerUUID, milestoneIndex);

                            player.openInventory(plugin.getMilestonesInventory().createInventory(player));

                            plugin.getMessageManager().sendMessage(player, "MILESTONE_CLAIMED");
                        }
                    } catch (SQLException e) {
                        Bukkit.getLogger().warning("An error occurred for player " + player.getName() + " while claiming a milestone");
                        e.printStackTrace();
                    }
                });
    }
}
