package me.fergs.phantomvoting;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import me.fergs.phantomvoting.commands.AdminCommands;
import me.fergs.phantomvoting.commands.PlayerCommands;
import me.fergs.phantomvoting.config.ConfigurationManager;
import me.fergs.phantomvoting.database.VoteStorage;
import me.fergs.phantomvoting.inventories.LeaderboardInventory;
import me.fergs.phantomvoting.inventories.MilestonesInventory;
import me.fergs.phantomvoting.inventories.StreaksInventory;
import me.fergs.phantomvoting.listeners.InventoryClickListener;
import me.fergs.phantomvoting.listeners.PlayerListener;
import me.fergs.phantomvoting.listeners.VoteReceiveListener;
import me.fergs.phantomvoting.listeners.modules.BossbarEventsListener;
import me.fergs.phantomvoting.managers.*;
import me.fergs.phantomvoting.modules.bossbar.BossbarManager;
import me.fergs.phantomvoting.utils.ConsoleUtil;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public final class PhantomVoting extends JavaPlugin {
    private static PhantomVoting instance;
    private ListenerManager<PhantomVoting> listenerManager;
    private ConfigurationManager<PhantomVoting> configurationManager;
    private VoteStorage voteStorage;
    private MessageManager<PhantomVoting> messageManager;
    private VotePartyManager votePartyManager;
    private BossbarManager<PhantomVoting> bossbarManager;
    private PlayerManager<PhantomVoting> playerManager;
    private LeaderboardInventory<PhantomVoting> leaderboardInventory;
    private VoteReminderManager<PhantomVoting> voteReminderManager;
    private MilestonesInventory<PhantomVoting> milestonesInventory;
    private StreaksInventory<PhantomVoting> streaksInventory;
    /**
     * Called when the plugin is loaded.
     * This is where we register the Command API if it is not already loaded.
     */
    @Override
    public void onLoad() {
        if (!CommandAPI.isLoaded()) {
            CommandAPI.onLoad(new CommandAPIBukkitConfig(this).verboseOutput(false));
        }
    }
    @Override
    public void onEnable() {
        instance = this;
        ConsoleUtil.printAsciiArt();
        listenerManager = new ListenerManager<>(this);
        listenerManager.registerListeners(
                VoteReceiveListener.class,
                BossbarEventsListener.class,
                InventoryClickListener.class,
                PlayerListener.class
        );
        configurationManager = new ConfigurationManager<>(this);
        configurationManager.loadConfigs(
                "config",
                "messages",
                "voteparty",
                "modules",
                "storage",
                "modules/bossbar",
                "modules/vote_reminder",
                "menus/leaderboard",
                "menus/streaks",
                "menus/milestones");

        configurationManager.loadModules();
        messageManager = new MessageManager<>(this, configurationManager);
        voteStorage = new VoteStorage("PhantomVoting", configurationManager.getConfig("storage"));

        try {
            voteStorage.loadMilestones();
            voteStorage.loadStreaks();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        votePartyManager = new VotePartyManager(this);
        leaderboardInventory = new LeaderboardInventory<>(this);
        playerManager = new PlayerManager<>(this);

        new PlaceholderManager(voteStorage, votePartyManager).register();

        new PlayerCommands().register(this);
        new AdminCommands().register(this);

        if (configurationManager.isModuleEnabled("bossbar")) {
            bossbarManager = new BossbarManager<>(this);
        }
        if (configurationManager.isModuleEnabled("VoteReminder")) {
            voteReminderManager = new VoteReminderManager<>(this);
            voteReminderManager.reloadTask("VoteReminder");
        }
        if (configurationManager.isModuleEnabled("Milestones")) {
            milestonesInventory = new MilestonesInventory<>(this);
        }

        if (configurationManager.isModuleEnabled("Streaks-Menu")) {
            streaksInventory = new StreaksInventory<>(this);
        }

        new Metrics(this, 23888);
    }
    @Override
    public void onDisable() {
        try {
            voteStorage.saveMilestones();
            voteStorage.saveStreaks();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        voteStorage.close();
        if (voteReminderManager != null) {
            voteReminderManager.cancelAllTasks();
        }
    }
    /**
     * Gets the plugin instance.
     *
     * @return the plugin instance
     */
    public static PhantomVoting getInstance() {
        return instance;
    }/**
     * Gets the configuration manager.
     *
     * @return the configuration manager
     */
    public ConfigurationManager<PhantomVoting> getConfigurationManager() {
        return configurationManager;
    }
    /**
     * Gets the message manager.
     *
     * @return the message manager
     */
    public MessageManager<PhantomVoting> getMessageManager() {
        return messageManager;
    }
    /**
     * Gets the vote storage.
     *
     * @return the vote storage
     */
    public VoteStorage getVoteStorage() {
        return voteStorage;
    }
    /**
     * Gets the vote party manager.
     *
     * @return the vote party manager
     */
    public VotePartyManager getVotePartyManager() {
        return votePartyManager;
    }
    /**
     * Gets the listener manager.
     *
     * @return the listener manager
     */
    public ListenerManager<PhantomVoting> getListenerManager() {
        return listenerManager;
    }
    /**
     * Gets the player manager.
     *
     * @return the player manager
     */
    public PlayerManager<PhantomVoting> getPlayerManager() {
        return playerManager;
    }
    /**
     * Gets the bossbar manager.
     *
     * @return the bossbar manager
     */
    public BossbarManager<PhantomVoting> getBossbarManager() {
        return bossbarManager;
    }
    /**
     * Gets the leaderboard inventory.
     *
     * @return the leaderboard inventory
     */
    public LeaderboardInventory<PhantomVoting> getLeaderboardInventory() {
        return leaderboardInventory;
    }
    /**
     * Gets the vote reminder manager.
     *
     * @return the vote reminder manager
     */
    public VoteReminderManager<PhantomVoting> getVoteReminderManager() {
        return voteReminderManager;
    }
    /**
     * Gets the milestones inventory.
     *
     * @return the milestones inventory
     */
    public MilestonesInventory<PhantomVoting> getMilestonesInventory() {
        return milestonesInventory;
    }
    /**
     * Gets the streaks inventory.
     *
     * @return the streaks inventory
     */
    public StreaksInventory<PhantomVoting> getStreaksInventory() {
        return streaksInventory;
    }
}
