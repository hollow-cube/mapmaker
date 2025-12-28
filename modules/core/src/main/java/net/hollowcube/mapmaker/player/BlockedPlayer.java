package net.hollowcube.mapmaker.player;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

public record BlockedPlayer(@NotNull String playerId, @NotNull String username, @NotNull Instant blockedAt) {
}
