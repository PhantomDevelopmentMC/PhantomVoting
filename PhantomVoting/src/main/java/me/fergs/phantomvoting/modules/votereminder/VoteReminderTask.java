package me.fergs.phantomvoting.modules.votereminder;

import me.fergs.phantomvoting.PhantomVoting;
import me.fergs.phantomvoting.utils.Color;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

public class VoteReminderTask<T extends PhantomVoting> extends BukkitRunnable {

    private final T plugin;
    private String message;
    /**
     * Initializes the VoteReminderTask with the plugin and parsed message.
     *
     * @param plugin  The main plugin instance.
     * @param message The message to broadcast.
     */
    public VoteReminderTask(T plugin, String message) {
        this.plugin = plugin;
        this.message = message;
        startTask();
    }
    /**
     * The task logic that will be executed on each run.
     */
    @Override
    public void run() {
        String[] lines = message.split("\n");
        Bukkit.getOnlinePlayers().forEach(player -> {
            for (String line : lines) {
                player.sendMessage(Color.hex(line));
            }
        });
    }
    /**
     * Starts the task with the interval defined in the configuration.
     */
    public void startTask() {
        runTaskTimer(plugin, 0, 20 * plugin.getConfigurationManager().getConfig("modules/vote_reminder").getLong("Interval", 3600));
    }
    /**
     * Cancels the task.
     */
    public void reload() {
        cancel();
        plugin.getConfigurationManager().getConfig("modules/vote_reminder").getString("Message");
        startTask();
    }
}
