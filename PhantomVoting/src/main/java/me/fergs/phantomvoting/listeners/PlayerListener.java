package me.fergs.phantomvoting.listeners;

import me.fergs.phantomvoting.PhantomVoting;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
    /**
     * Event handler for when a player joins the server.
     *
     * @param event the event
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        PhantomVoting.getInstance().getPlayerManager().addPlayer(event.getPlayer());
    }
    /**
     * Event handler for when a player leaves the server.
     *
     * @param event the event
     */
    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        PhantomVoting.getInstance().getPlayerManager().removePlayer(event.getPlayer());
    }
}
