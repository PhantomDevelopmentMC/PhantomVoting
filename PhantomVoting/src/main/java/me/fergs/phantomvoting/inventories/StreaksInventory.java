package me.fergs.phantomvoting.inventories;

import me.fergs.phantomvoting.PhantomVoting;
import me.fergs.phantomvoting.config.YamlConfigFile;
import me.fergs.phantomvoting.inventories.holders.StreaksInventoryHolder;
import me.fergs.phantomvoting.inventories.interfaces.InventoryInterface;
import me.fergs.phantomvoting.objects.InventoryFiller;
import me.fergs.phantomvoting.utils.Color;
import me.fergs.phantomvoting.utils.InventoryUtil;
import me.fergs.phantomvoting.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class StreaksInventory<T extends PhantomVoting> implements InventoryInterface {
    private final T plugin;
    private int inventorySize;
    private String inventoryTitle;
    private List<InventoryFiller> fillers = new ArrayList<>();
    private YamlConfigFile config;
    /**
     * Creates a new milestones inventory.
     *
     * @param plugin The plugin instance.
     */
    public StreaksInventory(T plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    /**
     * Loads the configuration.
     */
    private void loadConfig() {
        config = plugin.getConfigurationManager().getConfig("menus/streaks");
        this.inventorySize = config.getInt("Streaks.size", 27);
        this.inventoryTitle = Color.hex(config.getString("Streaks.title", "&8Vote Streaks"));

        loadFillers();
    }
    /**
     * Creates the inventory for the player.
     *
     * @param player The player to create the inventory for.
     * @return The inventory.
     */
    @Override
    public Inventory createInventory(Player player) {
        Inventory inventory = Bukkit.createInventory(new StreaksInventoryHolder(inventoryTitle), inventorySize, inventoryTitle);
        UUID playerUUID = player.getUniqueId();

        fillers.forEach(filler ->
                filler.getSlots().forEach(slots ->
                        slots.forEach(slot -> inventory.setItem(slot, filler.getItem()))
                )
        );

        ConfigurationSection menuSection = config.getConfigurationSection("Streaks.menu");
        if (menuSection == null) return inventory;

        boolean delayEnabled = config.getBoolean("Streaks.settings.use-delay", false);
        long delayTicks = config.getLong("Streaks.settings.delay-ticks", 10L);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Runnable> tasks = new ArrayList<>();
            int playerStreak = plugin.getVoteStorage().getPlayerStreak(playerUUID);
            try {
                for (String key : menuSection.getKeys(false)) {
                    ConfigurationSection milestoneConfig = menuSection.getConfigurationSection(key);
                    int requiredStreak = milestoneConfig.getInt("streak-required");
                    boolean isClaimed = plugin.getVoteStorage().isStreakClaimed(playerUUID, Integer.parseInt(key.substring(1)));

                    ItemStack item;
                    if (isClaimed) {
                        item = loadItem(milestoneConfig.getConfigurationSection("Claimed"));
                    } else if (playerStreak >= requiredStreak) {
                        item = loadItem(milestoneConfig.getConfigurationSection("Available"));
                    } else {
                        item = loadItem(milestoneConfig.getConfigurationSection("Locked"));
                    }

                    int slot = milestoneConfig.getInt("slot", -1);
                    if (slot >= 0 && slot < inventory.getSize()) {
                        tasks.add(() -> inventory.setItem(slot, item));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                if (delayEnabled) {
                    new BukkitRunnable() {
                        private int index = 0;
                        @Override
                        public void run() {
                            if (index >= tasks.size()) {
                                cancel();
                                return;
                            }
                            tasks.get(index).run();
                            index++;
                        }
                    }.runTaskTimerAsynchronously(plugin, 0, delayTicks);
                } else {
                    tasks.forEach(Runnable::run);
                }
            });
        });
        return inventory;
    }
    /**
     * Loads the fillers.
     */
    @Override
    public void loadFillers() {
        fillers = new ArrayList<>();
        ConfigurationSection fillerSection = config.getConfigurationSection("Streaks.filler");
        if (fillerSection != null) {
            for (String fillerKey : fillerSection.getKeys(false)) {
                ConfigurationSection fillerConfig = fillerSection.getConfigurationSection(fillerKey);
                assert fillerConfig != null;
                String material = fillerConfig.getString("material", "GRAY_STAINED_GLASS_PANE");
                String name = fillerConfig.getString("name", "&8");
                int customModelData = fillerConfig.getInt("custom-model-data", 0);
                int stackAmount = fillerConfig.getInt("item-amount", 1);
                boolean isGlowing = fillerConfig.getBoolean("glowing", false);
                List<String> lore = fillerConfig.getStringList("lore");
                List<String> slotRanges = fillerConfig.getStringList("slots");

                ItemStack fillerItem = ItemBuilder.create(Material.valueOf(material))
                        .setName(name)
                        .setLore(lore)
                        .setCustomModelData(customModelData)
                        .setItemAmount(stackAmount)
                        .setGlowing(isGlowing)
                        .build();

                List<List<Integer>> slots = InventoryUtil.parseSlotRanges(slotRanges);
                fillers.add(new InventoryFiller(fillerItem, slots));
            }
        }
    }
    /**
     * Loads an item from the configuration.
     *
     * @param section The configuration section.
     * @return The item.
     */
    private ItemStack loadItem(ConfigurationSection section) {
        String material = section.getString("material", "STONE");
        String name = section.getString("name", "&fDefault");
        List<String> lore = section.getStringList("lore");
        int customModelData = section.getInt("custom-model-data", 0);
        int stackAmount = section.getInt("item-amount", 1);
        boolean isGlowing = section.getBoolean("glowing", false);
        Optional<List<String>> itemFlagsOptional = Optional.of(section.getStringList("flags"));
        Optional<String> skullBase64 = Optional.ofNullable(section.getString("base64"));

        return ItemBuilder.create(Material.valueOf(material))
                .setSkullTexture(skullBase64.orElse(null))
                .setName(name)
                .setLore(lore)
                .setCustomModelData(customModelData)
                .setItemAmount(stackAmount)
                .setGlowing(isGlowing)
                .addItemFlags(itemFlagsOptional.orElse(null))
                .build();
    }
    /**
     * Reloads the inventory.
     */
    @Override
    public void reloadInventory() {
        loadConfig();
    }
}
