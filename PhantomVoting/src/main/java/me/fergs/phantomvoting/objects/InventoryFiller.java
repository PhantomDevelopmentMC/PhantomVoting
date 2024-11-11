package me.fergs.phantomvoting.objects;

import org.bukkit.inventory.ItemStack;

import java.util.List;

public class InventoryFiller {
    private final ItemStack item;
    private final List<List<Integer>> slots;

    public InventoryFiller(ItemStack item, List<List<Integer>> slots) {
        this.item = item;
        this.slots = slots;
    }

    public ItemStack getItem() {
        return item;
    }

    public List<List<Integer>> getSlots() {
        return slots;
    }
}