package net.hollowcube.terraform.util.transformations;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a transformation that applies multiple transformations to a schematic.
 */
public record MultiSchematicTransformation(@NotNull SchematicTransformation... transformations) implements SchematicTransformation {

    @Override
    public @NotNull Block apply(@NotNull Block block) {
        for (SchematicTransformation transformation : this.transformations) {
            block = transformation.apply(block);
        }
        return block;
    }

    @Override
    public @NotNull Point apply(@NotNull Point point, @NotNull Point size, @NotNull Point pivot) {
        for (SchematicTransformation transformation : this.transformations) {
            point = transformation.apply(point, size, pivot);
        }
        return point;
    }
}
