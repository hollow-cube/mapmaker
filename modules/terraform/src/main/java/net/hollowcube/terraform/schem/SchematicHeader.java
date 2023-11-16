package net.hollowcube.terraform.schem;

import net.minestom.server.coordinate.Point;
import org.jetbrains.annotations.NotNull;

public record SchematicHeader(
        @NotNull String name,
        @NotNull Point dimensions,
        int size // in bytes
) {
}
