package me.fergs.phantomvoting.managers;

import me.fergs.phantomvoting.PhantomVoting;
import me.fergs.phantomvoting.modules.votereminder.VoteReminderTask;

import org.bukkit.scheduler.BukkitRunnable;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages the vote reminder tasks.
 *
 * @param <T> The main plugin class.
 */
public class VoteReminderManager<T extends PhantomVoting> {
    private final T plugin;
    private final Map<String, VoteReminderTask> tasks = new HashMap<>();
    /**
     * Creates a new VoteReminderManager instance.
     *
     * @param plugin The main plugin instance.
     */
    public VoteReminderManager(T plugin) {
        this.plugin = plugin;
    }
    /**
     * Reloads the task with the specified key.
     *
     * @param key The key of the task.
     */
    public void reloadTask(String key) {
        VoteReminderTask currentTask = tasks.get(key);
        if (currentTask != null) {
            currentTask.cancel();
        }
        String message = plugin.getConfigurationManager().getConfig("modules/vote_reminder").getString("Message");
        long interval = plugin.getConfigurationManager().getConfig("modules/vote_reminder").getLong("Interval", 3600);

        VoteReminderTask newTask = new VoteReminderTask(message);
        newTask.runTaskTimerAsynchronously(plugin, 0, 20 * interval);

        tasks.put(key, newTask);
    }
    /**
     * Cancels all tasks.
     */
    public void cancelAllTasks() {
        tasks.values().forEach(BukkitRunnable::cancel);
        tasks.clear();
    }
}