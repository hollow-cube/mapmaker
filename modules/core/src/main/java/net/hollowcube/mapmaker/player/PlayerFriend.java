package net.hollowcube.mapmaker.player;

import net.hollowcube.common.util.RuntimeGson;

import java.time.Instant;

@RuntimeGson
public record PlayerFriend(
    String playerId,
    String username,
    boolean online,
    Instant lastOnline,
    Instant friendsSince
) {
}
