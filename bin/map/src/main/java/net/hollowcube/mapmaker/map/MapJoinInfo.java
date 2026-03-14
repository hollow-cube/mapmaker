package net.hollowcube.mapmaker.map;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.NotNull;

@RuntimeGson
public record MapJoinInfo(
    String playerId,
    String mapId,
    String state // building, etc
) {
}
