package me.fergs.phantomvoting.listeners.modules;

import me.fergs.phantomvoting.PhantomVoting;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class BossbarEventsListener implements Listener {
    /**
     * Handles a player join event.
     *
     * @param event the player join event
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        PhantomVoting.getInstance().getBossbarManager().onPlayerJoin(event);
    }
    /**
     * Handles a player leave event.
     *
     * @param event the player leave event
     */
    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        PhantomVoting.getInstance().getBossbarManager().onPlayerLeave(event);
    }
}
