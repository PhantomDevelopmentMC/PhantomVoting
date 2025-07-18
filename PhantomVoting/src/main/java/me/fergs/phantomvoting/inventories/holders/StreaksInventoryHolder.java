package me.fergs.phantomvoting.inventories.holders;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class StreaksInventoryHolder implements InventoryHolder {
    private final int page;
    public StreaksInventoryHolder(int page) { this.page = page; }
    public Inventory getInventory() { return null; }
    public int getPage() { return page; }
}
