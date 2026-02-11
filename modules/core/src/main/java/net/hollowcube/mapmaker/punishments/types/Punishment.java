package net.hollowcube.mapmaker.punishments.types;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

@RuntimeGson
public record Punishment(@NotNull String playerId, @NotNull String executorId, @NotNull PunishmentType type,
                         @NotNull Instant createdAt, @Nullable String ladderId, @NotNull String comment,
                         @Nullable Instant expiresAt, @Nullable String revokedBy, @Nullable Instant revokedAt,
                         @Nullable String revokedReason) {
}
