package me.fergs.phantomvoting;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import me.fergs.phantomvoting.commands.AdminCommands;
import me.fergs.phantomvoting.commands.PlayerCommands;
import me.fergs.phantomvoting.config.ConfigurationManager;
import me.fergs.phantomvoting.database.VoteStorage;
import me.fergs.phantomvoting.inventories.LeaderboardInventory;
import me.fergs.phantomvoting.listeners.InventoryClickListener;
import me.fergs.phantomvoting.listeners.VoteReceiveListener;
import me.fergs.phantomvoting.listeners.modules.BossbarEventsListener;
import me.fergs.phantomvoting.managers.ListenerManager;
import me.fergs.phantomvoting.managers.MessageManager;
import me.fergs.phantomvoting.managers.PlaceholderManager;
import me.fergs.phantomvoting.managers.VotePartyManager;
import me.fergs.phantomvoting.modules.bossbar.BossbarManager;
import me.fergs.phantomvoting.modules.votereminder.VoteReminderTask;
import org.bukkit.plugin.java.JavaPlugin;

public final class PhantomVoting extends JavaPlugin {
    private static PhantomVoting instance;
    private ListenerManager<PhantomVoting> listenerManager;
    private ConfigurationManager<PhantomVoting> configurationManager;
    private VoteStorage voteStorage;
    private MessageManager messageManager;
    private VotePartyManager votePartyManager;
    private BossbarManager<PhantomVoting> bossbarManager;
    private LeaderboardInventory<PhantomVoting> leaderboardInventory;
    private VoteReminderTask<PhantomVoting> voteReminderTask;
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
        listenerManager = new ListenerManager<>(this);
        listenerManager.registerListeners(
                VoteReceiveListener.class,
                BossbarEventsListener.class,
                InventoryClickListener.class
        );
        configurationManager = new ConfigurationManager<>(this);
        configurationManager.loadConfigs(
                "config",
                "messages",
                "voteparty",
                "modules",
                "modules/bossbar",
                "modules/vote_reminder",
                "menus/leaderboard");

        configurationManager.loadModules();
        messageManager = new MessageManager(configurationManager);
        voteStorage = new VoteStorage("PhantomVoting");
        votePartyManager = new VotePartyManager(this);
        leaderboardInventory = new LeaderboardInventory<>(this);

        new PlaceholderManager(voteStorage, votePartyManager).register();

        new PlayerCommands().register(this);
        new AdminCommands().register(this);

        if (configurationManager.isModuleEnabled("bossbar")) {
            bossbarManager = new BossbarManager<>(this);
        }
        if (configurationManager.isModuleEnabled("VoteReminder")) {
            voteReminderTask = new VoteReminderTask<>(this, configurationManager.getConfig("modules/vote_reminder").getString("Message"));
        }
    }
    @Override
    public void onDisable() {
        voteStorage.close();
    }
    /**
     * Gets the plugin instance.
     *
     * @return the plugin instance
     */
    public static PhantomVoting getInstance() {
        return instance;
    }
    /**
     * Gets the listener registry.
     *
     * @return the listener registry
     */
    public ListenerManager<PhantomVoting> getListenerRegistry() {
        return listenerManager;
    }
    /**
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
    public MessageManager getMessageManager() {
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
     * Gets the vote reminder task.
     *
     * @return the vote reminder task
     */
    public VoteReminderTask<PhantomVoting> getVoteReminderTask() {
        return voteReminderTask;
    }
}
