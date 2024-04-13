package net.hollowcube.mapmaker.player;

import net.hollowcube.mapmaker.session.PlayerSession;
import org.jetbrains.annotations.NotNull;

public record TransferSessionResponse(
        @NotNull PlayerDataV2 data,
        @NotNull PlayerSession session,
        boolean isJoin
) {
}
