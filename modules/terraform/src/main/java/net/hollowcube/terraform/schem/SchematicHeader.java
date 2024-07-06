package net.hollowcube.terraform.schem;

import net.minestom.server.coordinate.Point;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

public record SchematicHeader(
        @NotNull String name,
        int size, // in bytes
        @UnknownNullability Point dimensions,
        @UnknownNullability String fileType
) {
}
