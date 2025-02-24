package me.fergs.phantomvoting.managers;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.fergs.phantomvoting.database.VoteStorage;
import me.fergs.phantomvoting.enums.PlaceholderType;
import me.fergs.phantomvoting.utils.ConsoleUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlaceholderManager extends PlaceholderExpansion {

    private final VoteStorage voteStorage;
    private final VotePartyManager votePartyManager;
    /**
     * Creates a new PlaceholderManager.
     *
     * @param voteStorage      The vote storage.
     * @param votePartyManager The vote party manager.
     */
    public PlaceholderManager(VoteStorage voteStorage, VotePartyManager votePartyManager) {
        this.voteStorage = voteStorage;
        this.votePartyManager = votePartyManager;
    }
    /**
     * Registers the placeholders.
     *
     * @return Whether papi is enabled.
     */
    @Override
    public boolean register() {
        Bukkit.getLogger().info(ConsoleUtil.translateColors("&6[&e!&6] &eRegistered &fPlaceholder-API &eplaceholder(s)."));
        return super.register();
    }
    /**
     * Gets the plugin identifier.
     *
     * @return The plugin identifier.
     */
    @Override
    public @NotNull String getIdentifier() {
        return "phantomvoting";
    }
    /**
     * Gets the plugin author.
     *
     * @return The plugin author.
     */
    @Override
    public @NotNull String getAuthor() {
        return "f.";
    }
    /**
     * Gets the plugin version.
     *
     * @return The plugin version.
     */
    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }
    /**
     * Determines whether the placeholder should persist.
     *
     * @return Whether the placeholder should persist.
     */
    @Override
    public boolean persist() {
        return true;
    }
    /**
     * Gets the value of the placeholder.
     *
     * @param player     The player to get the placeholder value for.
     * @param identifier The placeholder identifier.
     * @return The value of the placeholder.
     */
    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (identifier.contains("top_player")) {
            return PlaceholderType.TOP_PLAYER.getValue(voteStorage, votePartyManager, player, identifier.substring(identifier.lastIndexOf("_") + 1));
        }

        if (identifier.contains("top_votes")) {
            return PlaceholderType.TOP_VOTES.getValue(voteStorage, votePartyManager, player, identifier.substring(identifier.lastIndexOf("_") + 1));
        }

        PlaceholderType type = PlaceholderType.fromIdentifier(identifier);
        if (type == null) {
            return "0";
        }

        return type.getValue(voteStorage, votePartyManager, player, null);
    }
}
