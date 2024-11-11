package me.fergs.phantomvoting.inventories.interfaces;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public interface InventoryInterface {

    Inventory createInventory(Player player);
    void loadFillers();
    void reloadInventory();
}
