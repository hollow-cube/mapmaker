package net.hollowcube.mapmaker.player;

import net.hollowcube.common.util.RuntimeGson;

import java.time.Instant;

@RuntimeGson
public record FriendRequest(String playerId, String username, Instant sentAt) {
}
