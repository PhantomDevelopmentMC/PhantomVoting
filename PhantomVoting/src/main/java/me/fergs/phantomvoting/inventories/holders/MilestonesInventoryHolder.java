package me.fergs.phantomvoting.inventories.holders;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class MilestonesInventoryHolder implements InventoryHolder {
    private final int page;
    public MilestonesInventoryHolder(int page) { this.page = page; }
    public Inventory getInventory() { return null; }
    public int getPage() { return page; }
}
