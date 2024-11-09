package me.fergs.phantomvoting.managers;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.fergs.phantomvoting.database.VoteStorage;
import me.fergs.phantomvoting.enums.PlaceholderType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlaceholderManager extends PlaceholderExpansion {

    private final VoteStorage voteStorage;
    private final VotePartyManager votePartyManager;

    public PlaceholderManager(VoteStorage voteStorage, VotePartyManager votePartyManager) {
        this.voteStorage = voteStorage;
        this.votePartyManager = votePartyManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "phantomvoting";
    }

    @Override
    public @NotNull String getAuthor() {
        return "f.";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        PlaceholderType type = PlaceholderType.fromIdentifier(identifier);
        if (type == null) {
            return "0";
        }
        return type.getValue(voteStorage, votePartyManager, player);
    }
}
