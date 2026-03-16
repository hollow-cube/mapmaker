package net.hollowcube.mapmaker.player;

import net.hollowcube.common.util.RuntimeGson;
import net.hollowcube.mapmaker.session.PlayerSession;

@RuntimeGson
public record TransferSessionResponse(
    PlayerData data,
    PlayerSession session,
    boolean isJoin
) {
}
