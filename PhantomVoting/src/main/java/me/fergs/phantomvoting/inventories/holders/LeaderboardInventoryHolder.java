package me.fergs.phantomvoting.inventories.holders;

import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class LeaderboardInventoryHolder implements InventoryHolder, Listener {
    private final String title;
    /**
     * Creates a new leaderboard inventory holder.
     *
     * @param title The title of the inventory.
     */
    public LeaderboardInventoryHolder(String title) {
        this.title = title;
    }
    /**
     * Gets the title of the inventory.
     *
     * @return The title.
     */
    @Override
    public Inventory getInventory() {
        return null;
    }
    /**
     * Gets the title of the inventory.
     *
     * @return The title.
     */
    public String getTitle() {
        return title;
    }
}
