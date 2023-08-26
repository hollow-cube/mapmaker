package dev.hollowcube.replay;

import net.minestom.server.coordinate.Point;
import org.jetbrains.annotations.NotNull;

public record ReplayMetadata(
        @NotNull Point origin
) {
}
