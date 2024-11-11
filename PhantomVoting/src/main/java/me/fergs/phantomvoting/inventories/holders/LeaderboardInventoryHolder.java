package me.fergs.phantomvoting.inventories.holders;

import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class LeaderboardInventoryHolder implements InventoryHolder, Listener {
    private final String title;

    public LeaderboardInventoryHolder(String title) {
        this.title = title;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }

    public String getTitle() {
        return title;
    }
}
