package net.hollowcube.mapmaker.session;

import net.hollowcube.common.util.RuntimeGson;
import net.hollowcube.mapmaker.player.PlayerSkin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.time.Instant;

@RuntimeGson
public record PlayerSession(
        @NotNull String playerId,
        @NotNull Instant createdAt,
        int protocolVersion,

        @NotNull String proxyId,
        @NotNull String serverId,

        boolean hidden,
        @NotNull String username,
        @NotNull PlayerSkin skin,

        @UnknownNullability Presence presence
) {
}
