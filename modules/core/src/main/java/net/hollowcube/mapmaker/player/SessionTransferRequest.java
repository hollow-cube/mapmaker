package net.hollowcube.mapmaker.player;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.NotNull;

@RuntimeGson
public record SessionTransferRequest(
        @NotNull String server,

        // Presence info
        @NotNull String type,
        @NotNull String state,
        @NotNull String map
) {
}
