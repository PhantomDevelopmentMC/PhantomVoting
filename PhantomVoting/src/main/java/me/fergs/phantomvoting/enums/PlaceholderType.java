package me.fergs.phantomvoting.enums;

import me.fergs.phantomvoting.database.VoteStorage;
import me.fergs.phantomvoting.managers.VotePartyManager;
import me.fergs.phantomvoting.objects.PlayerVoteData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public enum PlaceholderType {
    /**
     * The placeholder type for the player's total vote count.
     */
    DAILY_VOTES("daily_votes") {
        @Override
        public String getValue(VoteStorage voteStorage, VotePartyManager votePartyManager, Player player, String extra) {
            return String.valueOf(voteStorage.getPlayerVoteCount(player.getUniqueId(), "daily"));
        }
    },
    /**
     * The placeholder type for the player's weekly vote count.
     */
    WEEKLY_VOTES("weekly_votes") {
        @Override
        public String getValue(VoteStorage voteStorage, VotePartyManager votePartyManager, Player player, String extra) {
            return String.valueOf(voteStorage.getPlayerVoteCount(player.getUniqueId(), "weekly"));
        }
    },
    /**
     * The placeholder type for the player's monthly vote count.
     */
    MONTHLY_VOTES("monthly_votes") {
        @Override
        public String getValue(VoteStorage voteStorage, VotePartyManager votePartyManager, Player player, String extra) {
            return String.valueOf(voteStorage.getPlayerVoteCount(player.getUniqueId(), "monthly"));
        }
    },
    /**
     * The placeholder type for the player's yearly vote count.
     */
    YEARLY_VOTES("yearly_votes") {
        @Override
        public String getValue(VoteStorage voteStorage, VotePartyManager votePartyManager, Player player, String extra) {
            return String.valueOf(voteStorage.getPlayerVoteCount(player.getUniqueId(), "yearly"));
        }
    },
    /**
     * The placeholder type for the player's all-time vote count.
     */
    ALL_TIME_VOTES("all_time_votes") {
        @Override
        public String getValue(VoteStorage voteStorage, VotePartyManager votePartyManager, Player player, String extra) {
            return String.valueOf(voteStorage.getPlayerVoteCount(player.getUniqueId(), "all_time"));
        }
    },
    /**
     * The placeholder type for the current global vote count.
     */
    VOTE_PARTY_COUNT("vote_party_count") {
        @Override
        public String getValue(VoteStorage voteStorage, VotePartyManager votePartyManager, Player player, String extra) {
            return String.valueOf(votePartyManager.getCurrentVoteCount());
        }
    },
    /**
     * The placeholder type for the player's vote streak.
     */
    VOTE_STREAK("vote_streak") {
        @Override
        public String getValue(VoteStorage voteStorage, VotePartyManager votePartyManager, Player player, String extra) {
            return String.valueOf(voteStorage.getPlayerStreak(player.getUniqueId()));
        }
    },
    /**
     * The placeholder type for the player's vote party position.
     */
    PLAYER_POSITION("player_position") {
        @Override
        public String getValue(VoteStorage voteStorage, VotePartyManager votePartyManager, Player player, String extra) {
            return String.valueOf(voteStorage.getPlayerPosition(player.getUniqueId()));
        }
    },
    /**
     * The placeholder type for the vote party threshold.
     */
    VOTE_PARTY_THRESHOLD("vote_party_threshold") {
        @Override
        public String getValue(VoteStorage voteStorage, VotePartyManager votePartyManager, Player player, String extra) {
            return String.valueOf(votePartyManager.getVoteThreshold());
        }
    },
    /**
     * The placeholder type for the remaining votes until the vote party.
     */
    VOTE_UNTIL_PARTY("vote_until_party") {
        @Override
        public String getValue(VoteStorage voteStorage, VotePartyManager votePartyManager, Player player, String extra) {
            return String.valueOf(votePartyManager.getVoteThreshold() - votePartyManager.getCurrentVoteCount());
        }
    },
    /**
     * The placeholder type for the top player's name at a specific position.
     */
    TOP_PLAYER("top_player") {
        @Override
        public String getValue(VoteStorage voteStorage, VotePartyManager votePartyManager, Player player, String extra) {
            if (extra == null) return "None";
            try {
                int position = Integer.parseInt(extra);
                PlayerVoteData data = voteStorage.getTopPlayerAt(position);
                return data != null ? Bukkit.getOfflinePlayer(data.getUuid()).getName() : "None";
            } catch (NumberFormatException e) {
                return "None";
            }
        }
    },
    /**
     * The placeholder type for the top player's vote count at a specific position.
     */
    TOP_VOTES("top_votes") {
        @Override
        public String getValue(VoteStorage voteStorage, VotePartyManager votePartyManager, Player player, String extra) {
            if (extra == null) return "None";
            try {
                int position = Integer.parseInt(extra);
                PlayerVoteData data = voteStorage.getTopPlayerAt(position);
                return data != null ? String.valueOf(data.getVoteCount()) : "0";
            } catch (NumberFormatException e) {
                return "0";
            }
        }
    };
    /**
     * The identifier for the placeholder.
     */
    private final String identifier;
    /**
     * Creates a new placeholder type.
     *
     * @param identifier The identifier for the placeholder.
     */
    PlaceholderType(String identifier) {
        this.identifier = identifier;
    }
    /**
     * Gets the identifier for the placeholder.
     *
     * @return The identifier for the placeholder.
     */
    public String getIdentifier() {
        return identifier;
    }
    /**
     * Gets the value for the placeholder.
     *
     * @param voteStorage      The vote storage instance.
     * @param votePartyManager The vote party manager instance.
     * @param player           The player to get the value for.
     * @param extra            Any extra data to use.
     * @return The value for the placeholder.
     */
    public abstract String getValue(VoteStorage voteStorage, VotePartyManager votePartyManager, Player player, String extra);
    /**
     * Gets the placeholder type from an identifier.
     *
     * @param identifier The identifier to get the placeholder type for.
     * @return The placeholder type, or null if not found.
     */
    public static PlaceholderType fromIdentifier(String identifier) {
        for (PlaceholderType type : values()) {
            if (type.getIdentifier().equals(identifier)) {
                return type;
            }
        }
        return null;
    }
}
