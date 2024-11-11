package me.fergs.phantomvoting.objects;

import java.util.UUID;

public class PlayerVoteData {
    private final UUID uuid;
    private final int voteCount;

    public PlayerVoteData(UUID uuid, int voteCount) {
        this.uuid = uuid;
        this.voteCount = voteCount;
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getVoteCount() {
        return voteCount;
    }
}
