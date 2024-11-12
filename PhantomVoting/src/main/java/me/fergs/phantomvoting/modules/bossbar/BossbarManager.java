package me.fergs.phantomvoting.modules.bossbar;

import me.fergs.phantomvoting.PhantomVoting;
import me.fergs.phantomvoting.utils.Color;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages the bossbar for the vote party.
 * @param <T> the plugin instance
 */
public class BossbarManager<T extends PhantomVoting> {
    private final T plugin;
    private final Map<Player, BossBar> activeBossBars = new HashMap<>();
    private int votesRequired;
    private String title;
    private String completionSound;
    private final boolean isModuleEnabled;
    /**
     * Creates a new bossbar manager.
     * @param plugin the plugin instance
     */
    public BossbarManager(T plugin) {
        this.plugin = plugin;
        this.isModuleEnabled = true;
        loadConfig();
    }
    /**
     * Loads the bossbar configuration.
     */
    public void loadConfig() {
        if (!isModuleEnabled) return;

        ConfigurationSection bossbarConfig = plugin.getConfigurationManager().getConfig("modules/bossbar").getConfigurationSection("");
        if (bossbarConfig != null) {
            title = bossbarConfig.getString("Title", "&6Vote Party Progress");
            votesRequired = bossbarConfig.getInt("VotesRequired", 100);
            completionSound = bossbarConfig.getString("CompletionSound", "ENTITY_PLAYER_LEVELUP");
        } else {
            plugin.getLogger().warning("Bossbar config is not found or configured properly.");
        }
    }
    /**
     * Updates the bossbar for all online players.
     */
    public void update() {
        if (!isModuleEnabled) return;
        int currentVotes = PhantomVoting.getInstance().getVotePartyManager().getCurrentVoteCount();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!activeBossBars.containsKey(player)) {
                addToPlayer(player);
            }
            BossBar bossBar = activeBossBars.get(player);
            double progress = (double) currentVotes / votesRequired;
            bossBar.setProgress(progress);
            bossBar.setTitle(Color.hex(title)
                    .replace("%current_votes%", String.valueOf(currentVotes))
                    .replace("%votes_required%", String.valueOf(votesRequired)));

            if (currentVotes >= votesRequired) {
                triggerVoteParty(player);
            }
        }
    }
    /**
     * Adds the bossbar to a player.
     * @param player the player to add the bossbar to
     */
    private void addToPlayer(Player player) {
        if (!isModuleEnabled) return;

        BossBar bossBar = createBossBar();
        activeBossBars.put(player, bossBar);
        assert bossBar != null;
        bossBar.addPlayer(player);
    }
    /**
     * Creates the bossbar.
     * @return the bossbar
     */
    private BossBar createBossBar() {
        if (!isModuleEnabled) return null;
        ConfigurationSection bossbarConfig = plugin.getConfigurationManager().getConfig("modules/bossbar").getConfigurationSection("");
        BarColor barColor = BarColor.GREEN;
        BarStyle barStyle = BarStyle.SOLID;

        assert bossbarConfig != null;
        String barColorConfig = bossbarConfig.getString("BarColor", "green").toUpperCase();
        String barStyleConfig = bossbarConfig.getString("BarStyle", "solid").toUpperCase();

        try {
            barColor = BarColor.valueOf(barColorConfig);
            barStyle = BarStyle.valueOf(barStyleConfig);
        } catch (IllegalArgumentException e) {
            Bukkit.getLogger().warning("Invalid BarColor or BarStyle in Bossbar config.");
        }

        String parsedTitle = title
                .replace("%current_votes%", String.valueOf(PhantomVoting.getInstance().getVotePartyManager().getCurrentVoteCount()))
                .replace("%votes_required%", String.valueOf(votesRequired));

        BossBar bossBar = Bukkit.createBossBar(parsedTitle, barColor, barStyle);
        bossBar.setVisible(true);
        return bossBar;
    }
    /**
     * Triggers the vote party for a player.
     * @param player the player to trigger the vote party for
     */
    private void triggerVoteParty(Player player) {
        if (!isModuleEnabled) return;
        player.playSound(player.getLocation(), Sound.valueOf(completionSound), 1.0f, 1.0f);
    }
    /**
     *  Adds the bossbar to a player.
     * @param event the player join event
     */
    public void onPlayerJoin(PlayerJoinEvent event) {
        addToPlayer(event.getPlayer());
    }
    /**
     * Removes the bossbar from a player.
     * @param event the player quit event
     */
    public void onPlayerLeave(PlayerQuitEvent event) {
        removeFromPlayer(event.getPlayer());
    }
    /**
     * Removes the bossbar from a player.
     * @param player the player to remove the bossbar from
     */
    private void removeFromPlayer(Player player) {
        if (activeBossBars.containsKey(player)) {
            BossBar bossBar = activeBossBars.get(player);
            bossBar.removePlayer(player);
            activeBossBars.remove(player);
        }
    }
}
