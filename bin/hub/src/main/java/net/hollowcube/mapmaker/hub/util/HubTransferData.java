package net.hollowcube.mapmaker.hub.util;

import net.hollowcube.common.util.RuntimeGson;
import net.minestom.server.coordinate.Pos;
import org.jetbrains.annotations.NotNull;

@RuntimeGson
public record HubTransferData(
        @NotNull Pos position,
        int slot
) {
}
