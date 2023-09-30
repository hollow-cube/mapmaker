package net.hollowcube.mapmaker.object;

import net.minestom.server.coordinate.Point;
import org.jetbrains.annotations.NotNull;

public record ObjectData(
        @NotNull String id,
        @NotNull ObjectType type,
        @NotNull Point pos
) {
}
