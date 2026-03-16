package net.hollowcube.mapmaker.punishments.types;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

@RuntimeGson
public record Punishment(
    String playerId,
    String executorId,
    PunishmentType type,
    Instant createdAt,
    @Nullable String ladderId,
    String comment,
    @Nullable Instant expiresAt,
    @Nullable String revokedBy,
    @Nullable Instant revokedAt,
    @Nullable String revokedReason
) {
}
