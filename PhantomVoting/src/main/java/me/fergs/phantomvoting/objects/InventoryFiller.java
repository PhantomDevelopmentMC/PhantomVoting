package me.fergs.phantomvoting.objects;

import org.bukkit.inventory.ItemStack;

import java.util.List;

public class InventoryFiller {
    private final ItemStack item;
    private final List<List<Integer>> slots;
    private final int page;
    /**
     * Creates a new inventory filler.
     *
     * @param item The item.
     * @param page The page number.
     * @param slots The slots.
     */
    public InventoryFiller(final ItemStack item, final int page, final List<List<Integer>> slots) {
        this.item = item;
        this.page = page;
        this.slots = slots;
    }
    /**
     * Creates a new inventory filler with a default page number of 1.
     *
     * @param item  The item.
     * @param slots The slots.
     */
    public InventoryFiller(final ItemStack item, final List<List<Integer>> slots) {
        this(item, 1, slots);
    }
    /**
     * Gets the page number for this filler.
     *
     * @return The page number.
     */
    public int getPage() {
        return page;
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