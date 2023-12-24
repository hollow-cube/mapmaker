package net.hollowcube.mapmaker.session;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.time.Instant;

public record PlayerSession(
        @NotNull String playerId,
        @NotNull Instant createdAt,

        @NotNull String proxyId,
        @NotNull String serverId,

        @UnknownNullability Presence presence
) {
}
