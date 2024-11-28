package me.fergs.phantomvoting.inventories;

import me.fergs.phantomvoting.PhantomVoting;
import me.fergs.phantomvoting.config.YamlConfigFile;
import me.fergs.phantomvoting.inventories.holders.MilestonesInventoryHolder;
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
import java.util.UUID;

public class MilestonesInventory<T extends PhantomVoting> implements InventoryInterface {

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
    public MilestonesInventory(T plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    /**
     * Loads the configuration.
     */
    private void loadConfig() {
        config = plugin.getConfigurationManager().getConfig("menus/milestones");
        this.inventorySize = config.getInt("Milestones.size", 27);
        this.inventoryTitle = Color.hex(config.getString("Milestones.title", "&8Vote Milestones"));

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
        Inventory inventory = Bukkit.createInventory(new MilestonesInventoryHolder(inventoryTitle), inventorySize, inventoryTitle);
        UUID playerUUID = player.getUniqueId();

        fillers.forEach(filler ->
                filler.getSlots().forEach(slots ->
                        slots.forEach(slot -> inventory.setItem(slot, filler.getItem()))
                )
        );

        ConfigurationSection menuSection = config.getConfigurationSection("Milestones.menu");
        if (menuSection == null) return inventory;

        boolean delayEnabled = config.getBoolean("Milestones.settings.use-delay", false);
        long delayTicks = config.getLong("Milestones.settings.delay-ticks", 10L);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Runnable> tasks = new ArrayList<>();
            try {
                for (String key : menuSection.getKeys(false)) {
                    ConfigurationSection milestoneConfig = menuSection.getConfigurationSection(key);
                    int requiredVotes = milestoneConfig.getInt("required-votes");
                    int playerVotes = plugin.getVoteStorage().getPlayerVoteCount(playerUUID, "all_time");
                    boolean isClaimed = plugin.getVoteStorage().isMilestoneClaimed(playerUUID, Integer.parseInt(key.substring(1)));

                    ItemStack item;
                    if (isClaimed) {
                        item = loadItem(milestoneConfig.getConfigurationSection("Claimed"));
                    } else if (playerVotes >= requiredVotes) {
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

            Bukkit.getScheduler().runTask(plugin, () -> {
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
                    }.runTaskTimer(plugin, 0, delayTicks);
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
        ConfigurationSection fillerSection = config.getConfigurationSection("Milestones.filler");
        if (fillerSection != null) {
            for (String fillerKey : fillerSection.getKeys(false)) {
                ConfigurationSection fillerConfig = fillerSection.getConfigurationSection(fillerKey);
                assert fillerConfig != null;
                String material = fillerConfig.getString("material", "GRAY_STAINED_GLASS_PANE");
                String name = fillerConfig.getString("name", "&8");
                List<String> lore = fillerConfig.getStringList("lore");
                List<String> slotRanges = fillerConfig.getStringList("slots");

                ItemStack fillerItem = ItemBuilder.create(Material.valueOf(material))
                        .setName(name)
                        .setLore(lore)
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

        return ItemBuilder.create(Material.valueOf(material))
                .setName(name)
                .setLore(lore)
                .build();
    }
    /**
     * Gets the player name from the cache or fetches it from the server.
     *
     * @param uuid The player UUID.
     * @return The player name.
     */
    @Override
    public void reloadInventory() {
        loadConfig();
    }
}
