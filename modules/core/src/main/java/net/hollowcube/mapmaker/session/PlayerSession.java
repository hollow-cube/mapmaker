package net.hollowcube.mapmaker.session;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;

public record PlayerSession(
        @NotNull String playerId,
        @NotNull Instant createdAt,

        @NotNull String proxyId,
        @NotNull String serverId
) {
}
