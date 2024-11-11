package me.fergs.phantomvoting.utils;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class InventoryUtil {
    /**
     * Parses a slot range string into a list of integers.
     *
     * @param slotRanges The slot ranges.
     * @return The list of integers.
     */
    // Parse slot ranges (e.g., '0-10', '18-26') into individual slot numbers
    public static List<List<Integer>> parseSlotRanges(List<String> slotRanges) {
        List<List<Integer>> allSlots = new ArrayList<>();
        for (String range : slotRanges) {
            String[] bounds = range.split("-");
            int start = Integer.parseInt(bounds[0]);
            int end = Integer.parseInt(bounds[1]);
            List<Integer> slots = new ArrayList<>();
            for (int i = start; i <= end; i++) {
                slots.add(i);
            }
            allSlots.add(slots);
        }
        return allSlots;
    }
    /**
     * Parses a slot range string into a list of integers.
     *
     * @param range The slot range.
     * @return The list of integers.
     */
    public static List<Integer> parseSlotRange(String range) {
        List<Integer> slots = new ArrayList<>();
        String[] bounds = range.split("-");
        int start = Integer.parseInt(bounds[0]);
        int end = Integer.parseInt(bounds[1]);

        for (int i = start; i <= end; i++) {
            slots.add(i);
        }
        return slots;
    }
    /**
     * Creates an item from the configuration.
     *
     * @param config The configuration.
     * @param path   The path to the item.
     * @return The item.
     */
    public static ItemStack createItem(ConfigurationSection config, String path, String... placeholders) {
        Material material = Material.valueOf(config.getString(path + ".material", "STONE"));
        String name = config.getString(path + ".name", "");
        List<String> lore = config.getStringList(path + ".lore");
        name = MessageParser.parseKeyedValues(name, placeholders);
        lore = lore.stream().map(line -> MessageParser.parseKeyedValues(line, placeholders)).collect(Collectors.toList());

        return ItemBuilder.create(material)
                .setName(name)
                .setLore(lore)
                .build();
    }
}
