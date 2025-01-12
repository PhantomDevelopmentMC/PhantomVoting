package me.fergs.phantomvoting.inventories.holders;

import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class StreaksInventoryHolder implements InventoryHolder, Listener {

    private final String title;
    /**
     * Creates a new leaderboard inventory holder.
     *
     * @param title The title of the inventory.
     */
    public StreaksInventoryHolder(String title) {
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
}
