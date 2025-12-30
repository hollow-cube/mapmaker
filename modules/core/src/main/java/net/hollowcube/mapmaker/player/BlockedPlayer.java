package net.hollowcube.mapmaker.player;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

@RuntimeGson
public record BlockedPlayer(@NotNull String playerId, @NotNull String username, @NotNull Instant blockedAt) {
}
