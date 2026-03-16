package net.hollowcube.mapmaker.player;

import net.hollowcube.common.util.RuntimeGson;

@RuntimeGson
public record SessionTransferRequest(
    String server,

    // Presence info
    String type,
    String state,
    String map
) {
}
