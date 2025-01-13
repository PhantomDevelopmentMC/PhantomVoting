package me.fergs.phantomvoting.utils;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ItemBuilder {

    private ItemStack item;
    private ItemMeta meta;
    /**
     * Creates a new ItemBuilder.
     *
     * @param material The material of the item.
     */
    private ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }
    /**
     * Creates a new ItemBuilder.
     *
     * @param material The material of the item.
     */
    public static ItemBuilder create(Material material) {
        return new ItemBuilder(material);
    }
    /**
     * Sets the name of the item.
     *
     * @param name The name of the item.
     * @return The ItemBuilder.
     */
    public ItemBuilder setName(String name) {
        if (meta != null && name != null) {
            meta.setDisplayName(Color.hex(name));
        }
        return this;
    }
    /**
     * Sets the skull texture of the item.
     *
     * @param base64 The base64 texture string.
     * @return The ItemBuilder.
     */
    public ItemBuilder setSkullTexture(String base64) {
        if (item.getType() == Material.PLAYER_HEAD && base64 != null) {
            this.item = SkullUtils.itemFromBase64(base64);
            this.meta = item.getItemMeta();
        }
        return this;
    }
    /**
     * Sets the lore of the item.
     *
     * @param lore The lore of the item.
     * @return The ItemBuilder.
     */
    public ItemBuilder setLore(List<String> lore) {
        if (meta != null && lore != null) {
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(Color.hex(line));
            }
            meta.setLore(coloredLore);
        }
        return this;
    }
    /**
     * Sets the custom model data of the item.
     *
     * @param customModelData The custom model data.
     * @return The ItemBuilder.
     */
    public ItemBuilder setCustomModelData(int customModelData) {
        if (meta != null && customModelData > 0) {
            meta.setCustomModelData(customModelData);
        }
        return this;
    }
    /**
     * Sets the glowing state of the item.
     *
     * @param glowing The glowing state.
     * @return The ItemBuilder.
     */
    public ItemBuilder setGlowing(boolean glowing) {
        if (meta != null) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            if (glowing) {
                meta.addEnchant(Enchantment.LUCK, 1, true);
            }
        }
        return this;
    }
    /**
     * Sets the amount of the item.
     *
     * @param amount The amount of the item.
     * @return The ItemBuilder.
     */
    public ItemBuilder setItemAmount(int amount) {
        if (amount > 0) {
            item.setAmount(amount);
        }
        if (amount > 64) {
            item.setAmount(64);
        }

        item.setAmount(amount);
        return this;
    }
    /**
     * Adds item flags to the item.
     *
     * @param flags The flags to add.
     * @return The ItemBuilder.
     */
    public ItemBuilder addItemFlags(List<String> flags) {
        if (meta != null && flags != null) {
            for (String flag : flags) {
                ItemFlag itemFlag = ItemFlag.valueOf(flag);
                meta.addItemFlags(itemFlag);
            }
        }
        return this;
    }
    /**
    /**
     * Builds the item.
     *
     * @return The built item.
     */
    public ItemStack build() {
        if (meta != null) {
            item.setItemMeta(meta);
        }
        return item;
    }
}
