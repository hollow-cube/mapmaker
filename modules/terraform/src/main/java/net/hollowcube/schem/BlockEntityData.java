package net.hollowcube.schem;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.coordinate.Point;
import org.jetbrains.annotations.NotNull;

public record BlockEntityData(
        @NotNull String id,
        @NotNull Point position,
        @NotNull CompoundBinaryTag data
) {
}
