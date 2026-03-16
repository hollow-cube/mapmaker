package net.hollowcube.mapmaker.session;

import net.hollowcube.common.util.RuntimeGson;
import net.hollowcube.mapmaker.player.PlayerSkin;
import org.jetbrains.annotations.UnknownNullability;

import java.time.Instant;

@RuntimeGson
public record PlayerSession(
    String playerId,
    Instant createdAt,
    int protocolVersion,

    String proxyId,
    String serverId,

    boolean hidden,
    String username,
    PlayerSkin skin,

    @UnknownNullability Presence presence
) {
}
