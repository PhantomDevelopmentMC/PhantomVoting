package me.fergs.phantomvoting.modules.votereminder;

import me.clip.placeholderapi.PlaceholderAPI;
import me.fergs.phantomvoting.utils.Color;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * A task that broadcasts a vote reminder message to all online players.
 */
public class VoteReminderTask extends BukkitRunnable {
    private final String message;
    private final String permission;
    /**
     * Initializes the VoteReminderTask with the plugin, message, and interval.
     *
     * @param message The message to broadcast.
     */
    public VoteReminderTask(String message, String permission) {
        this.message = message;
        this.permission = permission;
    }
    /**
     * The task logic that will be executed on each run.
     */
    @Override
    public void run() {
        String[] lines = message.split("\n");
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (!player.hasPermission(permission)) {
                return;
            }
            for (String line : lines) {
                player.sendMessage(Color.hex(PlaceholderAPI.setPlaceholders(player, line)));
            }
        });
    }
}
