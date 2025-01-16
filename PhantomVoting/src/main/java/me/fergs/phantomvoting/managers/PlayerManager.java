package me.fergs.phantomvoting.managers;

import me.fergs.phantomvoting.PhantomVoting;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class PlayerManager<T extends PhantomVoting> {
    private final T plugin;
    private final Set<Player> players = new HashSet<>();
    /**
     * Constructs a PlayerManager for managing players.
     *
     * @param plugin the plugin instance used for player management
     */
    public PlayerManager(final T plugin) {
        this.plugin = plugin;
    }

    /**
     * Adds a player to the set of players.
     *
     * @param player the player to add
     */
    public void addPlayer(final Player player) {
        this.players.add(player);
    }
    /**
     * Removes a player from the set of players.
     *
     * @param player the player to remove
     */
    public void removePlayer(final Player player) {
        this.players.remove(player);
    }
    /**
     * Retrieves the set of players.
     *
     * @return the set of players
     */
    public Set<Player> getPlayers() {
        return this.players;
    }
    /**
     * Retrieves the plugin instance used for player management.
     *
     * @return the plugin instance
     */
    public T getPlugin() {
        return this.plugin;
    }
}
