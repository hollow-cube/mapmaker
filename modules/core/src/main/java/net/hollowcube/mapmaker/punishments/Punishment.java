package net.hollowcube.mapmaker.punishments;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

public record Punishment(@NotNull String playerId, @NotNull String executorId, @NotNull PunishmentType type,
                         @NotNull Instant createdAt, @Nullable String ladderId, @NotNull String comment,
                         boolean superceded, @Nullable String revokedBy, @Nullable Instant revokedAt,
                         @Nullable String revokedReason) {
}
