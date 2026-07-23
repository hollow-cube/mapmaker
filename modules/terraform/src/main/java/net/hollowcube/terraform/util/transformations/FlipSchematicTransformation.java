package net.hollowcube.terraform.util.transformations;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a transformation that flips the schematic across specified axes.
 */
public record FlipSchematicTransformation(boolean x, boolean y, boolean z) implements SchematicTransformation {

    @Override
    public @NotNull Block apply(@NotNull Block block) {
        return block;
    }

    @Override
    public @NotNull Point apply(@NotNull Point point, @NotNull Point size, @NotNull Point pivot) {
        int px = pivot.blockX();
        int py = pivot.blockY();
        int pz = pivot.blockZ();

        return new Vec(
                this.x ? 2 * px - point.blockX() : point.blockX(),
                this.y ? 2 * py - point.blockY() : point.blockY(),
                this.z ? 2 * pz - point.blockZ() : point.blockZ()
        );
    }
}
