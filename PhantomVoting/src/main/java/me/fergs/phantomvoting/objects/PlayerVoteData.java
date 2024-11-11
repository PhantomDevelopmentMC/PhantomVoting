package me.fergs.phantomvoting.objects;

import java.util.UUID;

public class PlayerVoteData {
    private final UUID uuid;
    private final int voteCount;
    /**
     * Creates a new player vote data.
     *
     * @param uuid      The UUID.
     * @param voteCount The vote count.
     */
    public PlayerVoteData(UUID uuid, int voteCount) {
        this.uuid = uuid;
        this.voteCount = voteCount;
    }
    /**
     * Gets the UUID.
     *
     * @return The UUID.
     */
    public UUID getUuid() {
        return uuid;
    }
    /**
     * Gets the vote count.
     *
     * @return The vote count.
     */
    public int getVoteCount() {
        return voteCount;
    }
}
