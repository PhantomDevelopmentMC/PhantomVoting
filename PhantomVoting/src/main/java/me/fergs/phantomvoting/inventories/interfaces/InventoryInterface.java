package me.fergs.phantomvoting.inventories.interfaces;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public interface InventoryInterface {
    /**
     * Creates the inventory.
     *
     * @param player The player.
     * @return The inventory.
     */
    Inventory createInventory(Player player);
    /**
     * Loads the fillers for the inventory.
     */
    void loadFillers();
    /**
     * Reloads the inventory.
     */
    void reloadInventory();
}
