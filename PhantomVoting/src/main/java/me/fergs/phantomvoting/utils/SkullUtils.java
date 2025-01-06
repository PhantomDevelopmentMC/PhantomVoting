package me.fergs.phantomvoting.utils;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

public class SkullUtils {

    private static Method metaSetProfileMethod;
    private static Field metaProfileField;

    /**
     * Creates a new skull ItemStack, compatible with various Minecraft versions.
     *
     * @return The skull ItemStack.
     */
    public static ItemStack createSkull() {
        try {
            return new ItemStack(Material.valueOf("PLAYER_HEAD"));
        } catch (IllegalArgumentException e) {
            return new ItemStack(Material.valueOf("SKULL_ITEM"), 1, (short) 3);
        }
    }

    /**
     * Creates an ItemStack with a custom Base64 texture.
     *
     * @param base64 The Base64 texture string.
     * @return The customized skull ItemStack.
     */
    public static ItemStack itemFromBase64(String base64) {
        if (base64 == null) {
            Bukkit.getLogger().warning("Base64 string is null.");
        }
        ItemStack skull = createSkull();
        applyBase64Texture(skull, base64);
        return skull;
    }

    /**
     * Applies a Base64 texture to an existing skull ItemStack.
     *
     * @param item   The skull ItemStack.
     * @param base64 The Base64 texture string.
     */
    public static void applyBase64Texture(ItemStack item, String base64) {
        if (item.getType() != Material.PLAYER_HEAD) {
            throw new IllegalArgumentException("Item must be a PLAYER_HEAD.");
        }

        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (isModernVersion()) {
            PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), null);
            profile.setProperty(new ProfileProperty("textures", base64));
            meta.setPlayerProfile(profile);
        } else {
            setLegacyProfile(meta, base64);
        }

        item.setItemMeta(meta);
    }

    /**
     * Determines if the server is running a modern Minecraft version (1.21+).
     *
     * @return True if the server is modern, false otherwise.
     */
    private static boolean isModernVersion() {
        String version = Bukkit.getBukkitVersion();
        return version.startsWith("1.21") || version.compareTo("1.21") > 0;
    }

    /**
     * Sets a legacy profile for a SkullMeta or Skull block.
     *
     * @param meta   The SkullMeta.
     * @param base64 The Base64 texture string.
     */
    private static void setLegacyProfile(SkullMeta meta, String base64) {
        try {
            if (metaSetProfileMethod == null) {
                metaSetProfileMethod = meta.getClass().getDeclaredMethod("setProfile", GameProfile.class);
                metaSetProfileMethod.setAccessible(true);
            }
            metaSetProfileMethod.invoke(meta, makeGameProfile(base64));
        } catch (ReflectiveOperationException e) {
            try {
                if (metaProfileField == null) {
                    metaProfileField = meta.getClass().getDeclaredField("profile");
                    metaProfileField.setAccessible(true);
                }
                metaProfileField.set(meta, makeGameProfile(base64));
            } catch (ReflectiveOperationException ex) {
                ex.printStackTrace();
            }
        }
    }
    /**
     * Creates a GameProfile with a Base64 texture.
     *
     * @param base64 The Base64 texture string.
     * @return The GameProfile.
     */
    private static GameProfile makeGameProfile(String base64) {
        UUID uuid = new UUID((long)base64.substring(base64.length() - 20).hashCode(), (long)base64.substring(base64.length() - 10).hashCode());
        GameProfile profile = new GameProfile(uuid, "Player");
        profile.getProperties().put("textures", new Property("textures", base64));
        return profile;
    }
}


