package net.hollowcube.mapmaker.player;

import net.hollowcube.common.util.RuntimeGson;
import net.hollowcube.ipc.player.PlayerData;
import net.hollowcube.mapmaker.session.PlayerSession;
import org.jetbrains.annotations.NotNull;

@RuntimeGson
public record TransferSessionResponse(
        @NotNull PlayerData data,
        @NotNull PlayerSession session,
        boolean isJoin
) {
}
