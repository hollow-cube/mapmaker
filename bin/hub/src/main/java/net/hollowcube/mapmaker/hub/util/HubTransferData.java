package net.hollowcube.mapmaker.hub.util;

import net.hollowcube.common.util.RuntimeGson;
import net.minestom.server.coordinate.Pos;

@RuntimeGson
public record HubTransferData(
        Pos position,
        int slot
) {
}
