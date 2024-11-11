package me.fergs.phantomvoting.listeners;

import me.fergs.phantomvoting.inventories.holders.LeaderboardInventoryHolder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class InventoryClickListener implements Listener {
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof LeaderboardInventoryHolder) {
            event.setCancelled(true);
        }
    }
}
