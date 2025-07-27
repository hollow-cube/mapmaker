package net.hollowcube.mapmaker.map.util;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.NotNull;

@RuntimeGson
public record MapJoinInfo(
        @NotNull String playerId,
        @NotNull String mapId,
        @NotNull String state // building, etc
) {
}
