package me.fergs.phantomvoting.inventories;

import me.fergs.phantomvoting.PhantomVoting;
import me.fergs.phantomvoting.config.YamlConfigFile;
import me.fergs.phantomvoting.inventories.holders.LeaderboardInventoryHolder;
import me.fergs.phantomvoting.inventories.interfaces.InventoryInterface;
import me.fergs.phantomvoting.objects.InventoryFiller;
import me.fergs.phantomvoting.objects.PlayerVoteData;
import me.fergs.phantomvoting.utils.Color;
import me.fergs.phantomvoting.utils.InventoryUtil;
import me.fergs.phantomvoting.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardInventory<T extends PhantomVoting> implements InventoryInterface {

    private final T plugin;
    private int refreshInterval;
    private int inventorySize;
    private String inventoryTitle;
    private List<String> playerSlots = new ArrayList<>();
    private List<InventoryFiller> fillers = new ArrayList<>();
    private ItemStack playerPositionItem;
    private ItemStack nullHeadItem;
    private YamlConfigFile config;
    /**
     * Creates a new leaderboard inventory.
     *
     * @param plugin The plugin instance.
     */
    public LeaderboardInventory(T plugin) {
        this.plugin = plugin;
        loadConfig();
        startRefreshingLeaderboard();
    }
    /**
     * Loads the configuration.
     */
    private void loadConfig() {
        config = plugin.getConfigurationManager().getConfig("menus/leaderboard");

        this.refreshInterval = config.getInt("Settings.leaderboard_refresh", 240);
        this.playerSlots = config.getStringList("Leaderboard.player-slots");
        this.playerPositionItem = InventoryUtil.createItem(config, "Leaderboard.player-position");
        this.inventorySize = config.getInt("Leaderboard.size", 27);
        this.inventoryTitle = config.getString("Leaderboard.title", "&8Vote Top Leaderboard");
        this.nullHeadItem = InventoryUtil.createItem(config, "Leaderboard.null-head");

        loadFillers();
    }
    @Override
    public Inventory createInventory(Player player) {
        LeaderboardInventoryHolder holder = new LeaderboardInventoryHolder(inventoryTitle);
        Inventory inventory = Bukkit.createInventory(holder, inventorySize, Color.hex(inventoryTitle));
        fillers.forEach(filler -> {
            filler.getSlots().forEach(slots -> {
                slots.forEach(slot -> inventory.setItem(slot, filler.getItem()));
            });
        });

        List<PlayerVoteData> topPlayers = plugin.getVoteStorage().getTopPlayers();
        int slotIndex = 0;

        for (String slotRange : playerSlots) {
            List<Integer> slots = InventoryUtil.parseSlotRange(slotRange);
            for (Integer slot : slots) {
                if (slotIndex < topPlayers.size()) {
                    PlayerVoteData playerData = topPlayers.get(slotIndex);
                    ItemStack playerItem = createPlayerItem(playerData);
                    inventory.setItem(slot, playerItem);
                } else {
                    inventory.setItem(slot, nullHeadItem);
                }
                slotIndex++;
            }
        }
        int playerPosition = plugin.getVoteStorage().getPlayerPosition(player.getUniqueId());
        ItemStack positionItem = playerPositionItem.clone();
        ItemMeta meta = positionItem.getItemMeta();
        assert meta != null;
        meta.setDisplayName(Color.hex(meta.getDisplayName().replace("%position%", String.valueOf(playerPosition))));
        inventory.setItem(config.getInt("Leaderboard.player-position.slot"), positionItem);

        return inventory;
    }
    /**
     * Starts refreshing the leaderboard.
     */
    private void startRefreshingLeaderboard() {
        new BukkitRunnable() {
            @Override
            public void run() {
                refreshLeaderboardData();
            }
        }.runTaskTimer(plugin, 0L, refreshInterval * 20L);
    }
    /**
     * Refreshes the leaderboard data.
     */
    private void refreshLeaderboardData() {
        /* Refresh leaderboard data */
        // TODO: Implement this method
    }
    /**
     * Creates a player item for the leaderboard.
     *
     * @param playerData The player data.
     * @return The player item.
     */
    private ItemStack createPlayerItem(PlayerVoteData playerData) {
        String playerName = Bukkit.getOfflinePlayer(playerData.getUuid()).getName();
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        SkullMeta skullMeta = (SkullMeta) meta;
        skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(playerData.getUuid()));
        item.setItemMeta(skullMeta);
        meta.setDisplayName(Color.hex("&6&l[&e&l!&6&l] &e" + playerName + " &8(&7" + playerData.getVoteCount() + " Votes&8)"));
        item.setItemMeta(meta);
        return item;
    }
    /**
     * Loads the fillers from the configuration.
     */
    @Override
    public void loadFillers() {
        fillers = new ArrayList<>();

        ConfigurationSection fillerSection = config.getConfigurationSection("Leaderboard.filler");
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
     * Reloads the inventory.
     */
    @Override
    public void reloadInventory() {
        loadConfig();
    }
}
