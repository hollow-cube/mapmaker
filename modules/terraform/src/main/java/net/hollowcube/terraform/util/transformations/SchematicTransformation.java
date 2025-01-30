package net.hollowcube.terraform.util.transformations;

import net.hollowcube.common.types.Axis;
import net.hollowcube.schem.Rotation;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public sealed interface SchematicTransformation permits FlipSchematicTransformation, MultiSchematicTransformation, RotateSchematicTransformation {

    @NotNull Block apply(@NotNull Block block);

    @NotNull Point apply(@NotNull Point point, @NotNull Point size, @NotNull Point pivot);

    default @NotNull Point apply(@NotNull Point point, @NotNull Point size) {
        return apply(point, size, Vec.ZERO);
    }

    @NotNull
    @Contract("_, _, _ -> new")
    static SchematicTransformation flip(boolean x, boolean y, boolean z) {
        return new FlipSchematicTransformation(x, y, z);
    }

    @NotNull
    @Contract("_, _ -> new")
    static SchematicTransformation rotate(@NotNull Axis axis, @NotNull Rotation rotation) {
        return new RotateSchematicTransformation(axis, rotation);
    }

    @NotNull
    @Contract("_ -> new")
    static SchematicTransformation of(@NotNull SchematicTransformation... transformations) {
        return new MultiSchematicTransformation(transformations);
    }
}
