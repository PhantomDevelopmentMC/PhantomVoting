package me.fergs.phantomvoting.objects;

import org.bukkit.inventory.ItemStack;

import java.util.List;

public class InventoryFiller {
    private final ItemStack item;
    private final List<List<Integer>> slots;
    /**
     * Creates a new inventory filler.
     *
     * @param item  The item.
     * @param slots The slots.
     */
    public InventoryFiller(ItemStack item, List<List<Integer>> slots) {
        this.item = item;
        this.slots = slots;
    }
    /**
     * Gets the item.
     *
     * @return The item.
     */
    public ItemStack getItem() {
        return item;
    }
    /**
     * Gets the slots.
     *
     * @return The slots.
     */
    public List<List<Integer>> getSlots() {
        return slots;
    }
}