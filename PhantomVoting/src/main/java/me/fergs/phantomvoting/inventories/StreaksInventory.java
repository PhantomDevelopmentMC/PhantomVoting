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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class StreaksInventory implements InventoryInterface {
    private final PhantomVoting plugin;
    private int inventorySize;
    private String inventoryTitle;
    private List<InventoryFiller> fillers = new ArrayList<>();
    private YamlConfigFile config;
    private int maxPage;
    private ItemStack nextPageItem;
    private ItemStack prevPageItem;
    private int NEXT_SLOT;
    private int PREV_SLOT;
    private boolean isPagesEnabled;
    /**
     * Creates a new milestones inventory.
     *
     * @param plugin The plugin instance.
     */
    public StreaksInventory(PhantomVoting plugin) {
        this.plugin = plugin;
        cache();
    }
    /**
     * Loads the configuration.
     */
    private void cache() {
        this.config = plugin.getConfigurationManager().getConfig("menus/streaks");
        this.inventorySize = config.getInt("Streaks.size", 27);
        this.inventoryTitle = Color.hex(config.getString("Streaks.title", "&8Vote Streaks"));

        loadFillers();

        ConfigurationSection pages = config.getConfigurationSection("Streaks.settings.pages");
        this.isPagesEnabled = pages != null && pages.getBoolean("enabled", false);
        this.prevPageItem = buildButton(pages.getConfigurationSection("previous-page-item"));
        this.nextPageItem = buildButton(pages.getConfigurationSection("next-page-item"));
        this.PREV_SLOT = pages.getConfigurationSection("previous-page-item").getInt("slot");
        this.NEXT_SLOT = pages.getConfigurationSection("next-page-item").getInt("slot");
    }

    private ItemStack buildButton(ConfigurationSection sec) {
        if (sec == null) return new ItemStack(Material.AIR);
        Material mat = Material.valueOf(sec.getString("material","ARROW"));
        return new ItemBuilder(mat)
                .setName(Color.hex(sec.getString("name","")))
                .setLore(Color.hexList(sec.getStringList("lore")))
                .build();
    }

    /** Call this to open page #page for the player. */
    public void open(Player player, int page) {
        player.openInventory(createInventory(player, page));
    }

    @Override
    public Inventory createInventory(Player player) {
        // default to page 1 if someone calls this
        return createInventory(player, 1);
    }
    /**
     * Creates the inventory for the player.
     *
     * @param player The player to create the inventory for.
     * @param currentPage The current page number.
     * @return The inventory.
     */
    public Inventory createInventory(Player player, int currentPage) {
        final UUID playerUUID = player.getUniqueId();
        ConfigurationSection menuSec = config.getConfigurationSection("Streaks.menu");

        maxPage = menuSec.getKeys(false)
                .stream()
                .map(k -> menuSec.getConfigurationSection(k).getInt("page",1))
                .max(Integer::compareTo).orElse(1);

        String title = inventoryTitle
                .replace("%page%", String.valueOf(currentPage))
                .replace("%max%",  String.valueOf(maxPage));

        Inventory inv = Bukkit.createInventory(new StreaksInventoryHolder(currentPage), inventorySize, Color.hex(title));

        fillers.stream()
                .filter(f -> f.getPage() == currentPage)
                .forEach(filler -> filler.getSlots().forEach(range ->
                        range.forEach(idx -> inv.setItem(idx, filler.getItem()))
                ));

        if (isPagesEnabled) {
            inv.setItem(PREV_SLOT, prevPageItem);
            inv.setItem(NEXT_SLOT, nextPageItem);
        }

        boolean delayEnabled = config.getBoolean("Streaks.settings.use-delay", false);
        long delayTicks = config.getLong("Streaks.settings.delay-ticks", 10L);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Runnable> tasks = new ArrayList<>();
            int playerStreak = plugin.getVoteStorage().getPlayerStreak(playerUUID);
            for (String key : menuSec.getKeys(false)) {
                ConfigurationSection milestoneConfig = menuSec.getConfigurationSection(key);
                int page = milestoneConfig.getInt("page",1);
                if (page != currentPage) continue;
                int requiredStreak = milestoneConfig.getInt("streak-required");
                boolean isClaimed = plugin.getVoteStorage().isStreakClaimed(playerUUID, Integer.parseInt(key.substring(1)));

                ItemStack item;
                if (isClaimed) {
                    item = loadItem(milestoneConfig.getConfigurationSection("Claimed"), requiredStreak);
                } else if (playerStreak >= requiredStreak) {
                    item = loadItem(milestoneConfig.getConfigurationSection("Available"), requiredStreak);
                } else {
                    item = loadItem(milestoneConfig.getConfigurationSection("Locked"), requiredStreak);
                }

                int slot = milestoneConfig.getInt("slot", -1);
                if (slot >= 0 && slot < inv.getSize()) {
                    tasks.add(() -> inv.setItem(slot, item));
                }
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
        return inv;
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
                int page = fillerConfig.getInt("page", 1);

                ItemStack fillerItem = ItemBuilder.create(Material.valueOf(material))
                        .setName(name)
                        .setLore(lore)
                        .setCustomModelData(customModelData)
                        .setItemAmount(stackAmount)
                        .setGlowing(isGlowing)
                        .build();

                List<List<Integer>> slots = InventoryUtil.parseSlotRanges(slotRanges);
                fillers.add(new InventoryFiller(fillerItem, page, slots));
            }
        }
    }
    /**
     * Loads an item from the configuration.
     *
     * @param section The configuration section.
     * @return The item.
     */
    private ItemStack loadItem(ConfigurationSection section, int requiredStreak) {
        String material = section.getString("material", "STONE");
        String name = section.getString("name", "&fDefault");
        List<String> lore = section.getStringList("lore");
        int customModelData = section.getInt("custom-model-data", 0);
        int stackAmount = section.getInt("item-amount", 1);
        boolean isGlowing = section.getBoolean("glowing", false);
        Optional<List<String>> itemFlagsOptional = Optional.of(section.getStringList("flags"));
        Optional<String> skullBase64 = Optional.ofNullable(section.getString("base64"));

        lore.replaceAll(line -> line.replace("%required_streak%", String.valueOf(requiredStreak)));

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
        fillers.clear();
        cache();
        for (Player player : plugin.getPlayerManager().getPlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof StreaksInventoryHolder) {
                open(player, 1);
            }
        }
    }

    public int getPrevSlot() {
        return PREV_SLOT;
    }

    public int getNextSlot() {
        return NEXT_SLOT;
    }

    public int getMaxPage() {
        return maxPage;
    }
}
