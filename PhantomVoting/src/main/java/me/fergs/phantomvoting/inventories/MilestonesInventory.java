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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public class MilestonesInventory implements InventoryInterface {
    private final PhantomVoting plugin;
    private YamlConfigFile config;
    private int inventorySize;
    private String inventoryTitle;
    private List<InventoryFiller> fillers = new ArrayList<>();
    private ItemStack nextPageItem;
    private ItemStack prevPageItem;
    private int NEXT_SLOT;
    private int PREV_SLOT;

    public MilestonesInventory(PhantomVoting plugin) {
        this.plugin = plugin;
        cache();
    }

    public void cache() {
        this.config = plugin.getConfigurationManager().getConfig("menus/milestones");
        this.inventorySize = config.getInt("Milestones.size", 27);
        this.inventoryTitle = Color.hex(config.getString("Milestones.title", "&8Vote Milestones"));

        ConfigurationSection fillSec = config.getConfigurationSection("Milestones.filler");
        if (fillSec != null) {
            for (String key : fillSec.getKeys(false)) {
                ConfigurationSection s = fillSec.getConfigurationSection(key);
                ItemStack item = ItemBuilder.create(Material.valueOf(s.getString("material", "GRAY_STAINED_GLASS_PANE")))
                        .setName(Color.hex(s.getString("name", "&8")))
                        .setLore(Color.hexList(s.getStringList("lore")))
                        .setCustomModelData(s.getInt("custom-model-data", 0))
                        .setItemAmount(s.getInt("item-amount", 1))
                        .setGlowing(s.getBoolean("glowing", false))
                        .build();
                fillers.add(new InventoryFiller(item, InventoryUtil.parseSlotRanges(s.getStringList("slots"))));
            }
        }

        ConfigurationSection pages = config.getConfigurationSection("Milestones.settings.pages");
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

    /** Internal: builds the inventory for a given page. */
    private Inventory createInventory(Player player, int currentPage) {
        UUID playerUUID = player.getUniqueId();
        ConfigurationSection menuSec = config.getConfigurationSection("Milestones.menu");

        int maxPage = menuSec.getKeys(false)
                .stream()
                .map(k -> menuSec.getConfigurationSection(k).getInt("page",1))
                .max(Integer::compareTo).orElse(1);

        String title = inventoryTitle
                .replace("%page%", String.valueOf(currentPage))
                .replace("%max%",  String.valueOf(maxPage));

        Inventory inv = Bukkit.createInventory(new MilestonesInventoryHolder(1), inventorySize, Color.hex(title));

        fillers.forEach(f ->
                f.getSlots().forEach(range ->
                        range.forEach(idx -> inv.setItem(idx, f.getItem()))
                )
        );

        List<Runnable> tasks = new ArrayList<>();
        int playerVotes = plugin.getVoteStorage().getPlayerVoteCount(playerUUID, "all_time");

        for (String key : menuSec.getKeys(false)) {
            ConfigurationSection ms = menuSec.getConfigurationSection(key);
            int page = ms.getInt("page",1);
            if (page != currentPage) continue;

            int required = ms.getInt("required-votes");
            boolean claimed = plugin.getVoteStorage()
                    .isMilestoneClaimed(playerUUID, Integer.parseInt(key.substring(1)));
            String state = claimed ? "Claimed"
                    : (playerVotes >= required ? "Available" : "Locked");
            ConfigurationSection itemSec = ms.getConfigurationSection(state);
            if (itemSec == null) continue;

            ItemStack item = loadItem(itemSec, required);
            int slot = ms.getInt("slot", -1);
            if (slot >= 0 && slot < inventorySize) {
                tasks.add(() -> inv.setItem(slot, item));
            }
        }

        if (currentPage > 1) inv.setItem(PREV_SLOT, prevPageItem);
        if (currentPage < maxPage) inv.setItem(NEXT_SLOT, nextPageItem);

        boolean useDelay = config.getBoolean("Milestones.settings.use-delay", false);
        long delayTicks = config.getLong("Milestones.settings.delay-ticks", 10L);

        if (useDelay) {
            new BukkitRunnable() {
                int i = 0;
                @Override public void run() {
                    if (i >= tasks.size()) { cancel(); return; }
                    tasks.get(i++).run();
                }
            }.runTaskTimerAsynchronously(plugin, 0, delayTicks);
        } else {
            tasks.forEach(Runnable::run);
        }

        return inv;
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
    private ItemStack loadItem(ConfigurationSection section, int requiredVotes) {
        String material = section.getString("material", "STONE");
        String name = section.getString("name", "&fDefault");
        List<String> lore = section.getStringList("lore");
        int customModelData = section.getInt("custom-model-data", 0);
        int stackAmount = section.getInt("item-amount", 1);
        boolean isGlowing = section.getBoolean("glowing", false);
        Optional<List<String>> itemFlagsOptional = Optional.of(section.getStringList("flags"));
        Optional<String> skullBase64 = Optional.ofNullable(section.getString("base64"));

        lore.replaceAll(line -> line.replace("%votes_required%", String.valueOf(requiredVotes)));

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
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof MilestonesInventoryHolder) {
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
}
