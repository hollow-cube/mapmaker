package net.hollowcube.mapmaker.map;

import net.hollowcube.common.util.RuntimeGson;

@RuntimeGson
public record MapJoinInfo(
    String playerId,
    String mapId,
    String state // building, etc
) {
}
