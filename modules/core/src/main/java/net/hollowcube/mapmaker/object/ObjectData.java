package net.hollowcube.mapmaker.object;

import net.hollowcube.common.util.RuntimeGson;
import net.minestom.server.coordinate.Point;
import org.jetbrains.annotations.NotNull;

@RuntimeGson
public record ObjectData(
        @NotNull String id,
        @NotNull ObjectType type,
        @NotNull Point pos
) {
}
