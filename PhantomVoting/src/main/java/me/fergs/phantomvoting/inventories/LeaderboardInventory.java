package me.fergs.phantomvoting.inventories;

import me.fergs.phantomvoting.PhantomVoting;
import me.fergs.phantomvoting.config.YamlConfigFile;
import me.fergs.phantomvoting.inventories.holders.LeaderboardInventoryHolder;
import me.fergs.phantomvoting.inventories.interfaces.InventoryInterface;
import me.fergs.phantomvoting.objects.InventoryFiller;
import me.fergs.phantomvoting.objects.PlayerVoteData;
import me.fergs.phantomvoting.utils.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.stream.Collectors;

public class LeaderboardInventory<T extends PhantomVoting> implements InventoryInterface {

    private final T plugin;
    private final int refreshInterval;
    private long lastRefreshTime;
    private int inventorySize;
    private String inventoryTitle;
    private Set<String> playerSlots = new HashSet<>();
    private List<InventoryFiller> fillers = new ArrayList<>();
    private ItemStack nullHeadItem;
    private YamlConfigFile config;
    private List<PlayerVoteData> cachedTopPlayers = new ArrayList<>();
    private final Map<UUID, String> playerNameCache = new HashMap<>();
    /**
     * Creates a new leaderboard inventory.
     *
     * @param plugin The plugin instance.
     */
    public LeaderboardInventory(T plugin) {
        this.plugin = plugin;
        loadConfig();
        this.refreshInterval = config.getInt("Settings.leaderboard_refresh", 240);
        startRefreshingLeaderboard();
    }
    /**
     * Loads the configuration.
     */
    private void loadConfig() {
        config = plugin.getConfigurationManager().getConfig("menus/leaderboard");
        this.playerSlots = new HashSet<>(config.getStringList("Leaderboard.player-slots"));
        this.inventorySize = config.getInt("Leaderboard.size", 27);
        this.inventoryTitle = Color.hex(config.getString("Leaderboard.title", "&8Vote Top Leaderboard"));
        this.nullHeadItem = InventoryUtil.createItem(config, "Leaderboard.null-head");

        loadFillers();
    }
    /**
     * Creates the inventory.
     *
     * @param player The player.
     * @return The inventory.
     */
    @Override
    public Inventory createInventory(Player player) {
        LeaderboardInventoryHolder holder = new LeaderboardInventoryHolder(inventoryTitle);
        Inventory inventory = Bukkit.createInventory(holder, inventorySize, inventoryTitle);
        fillers.forEach(filler -> {
            filler.getSlots().forEach(slots -> {
                slots.forEach(slot -> inventory.setItem(slot, filler.getItem()));
            });
        });
        int slotIndex = 0;
        for (String slotRange : playerSlots) {
            List<Integer> slots = InventoryUtil.parseSlotRange(slotRange);
            for (Integer slot : slots) {
                if (slotIndex < cachedTopPlayers.size()) {
                    PlayerVoteData playerData = cachedTopPlayers.get(slotIndex);
                    ItemStack playerItem = createPlayerItem(playerData);
                    inventory.setItem(slot, playerItem);
                } else {
                    inventory.setItem(slot, nullHeadItem != null ? nullHeadItem : new ItemStack(Material.BEDROCK));
                }
                slotIndex++;
            }
        }
        int playerPosition = plugin.getVoteStorage().getPlayerPosition(player.getUniqueId());
        ItemStack positionItem = InventoryUtil.createItem(
                config,
                "Leaderboard.player-position",
                "%position%", String.valueOf(playerPosition),
                "%refresh_time%", FormatUtil.formatTimeStamp((getNextRefreshTime() - (System.currentTimeMillis() / 1000))),
                "%player%", getPlayerName(player.getUniqueId()));
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
        }.runTaskTimerAsynchronously(plugin, 0L, refreshInterval * 20L);
    }
    /**
     * Refreshes the leaderboard data and updates the cached top players.
     */
    private void refreshLeaderboardData() {
        cachedTopPlayers = plugin.getVoteStorage().getTopPlayers();
        plugin.getMessageManager().broadcastMessage("LEADERBOARD_REFRESH");
        lastRefreshTime = System.currentTimeMillis();
    }
    /**
     * Creates a player item for the leaderboard.
     *
     * @param playerData The player data.
     * @return The player item.
     */
    private ItemStack createPlayerItem(PlayerVoteData playerData) {
        UUID uuid = playerData.getUuid();
        String playerName = getPlayerName(uuid);
        int position = cachedTopPlayers.indexOf(playerData) + 1;
        int votes    = playerData.getVoteCount();

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return head;

        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        String name = offline.getName();
        if (name == null || name.isEmpty()) {
            meta.setOwner("Steve");
        } else {
            meta.setOwningPlayer(offline);
        }

        String display = Color.hex(config.getString("Leaderboard.player-head.name",
                        "&6&l[&e&l!&6&l] &e%player% &8(&7#%position%&8)")
                .replace("%player%",  playerName)
                .replace("%position%", String.valueOf(position))
                .replace("%votes%",    String.valueOf(votes))
        );
        meta.setDisplayName(display);

        List<String> lore = config.getStringList("Leaderboard.player-head.lore").stream()
                .map(line -> Color.hex(line
                        .replace("%player%",  playerName)
                        .replace("%position%", String.valueOf(position))
                        .replace("%votes%",    String.valueOf(votes))
                ))
                .collect(Collectors.toList());
        meta.setLore(lore);

        head.setItemMeta(meta);
        return head;
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
     * Gets the player name from the cache or fetches it from the server.
     *
     * @param uuid The player UUID.
     * @return The player name.
     */
    private String getPlayerName(UUID uuid) {
        return playerNameCache.computeIfAbsent(uuid, id -> {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(id);
            return offlinePlayer.getName();
        });
    }
    /**
     * Gets the next refresh time.
     *
     * @return The next refresh time.
     */
    public long getNextRefreshTime() {
        return (lastRefreshTime / 1000) + refreshInterval;
    }
    /**
     * Reloads the inventory.
     */
    @Override
    public void reloadInventory() {
        loadConfig();
    }
}
