package net.hollowcube.mapmaker.player;

import net.hollowcube.common.util.RuntimeGson;

import java.time.Instant;

@RuntimeGson
public record BlockedPlayer(String playerId, String username, Instant blockedAt) {
}
