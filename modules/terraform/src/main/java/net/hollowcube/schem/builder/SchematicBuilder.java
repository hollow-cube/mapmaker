package net.hollowcube.schem.builder;

import net.hollowcube.schem.Schematic;
import net.kyori.adventure.nbt.BinaryTag;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

public interface SchematicBuilder {

    static @NotNull SchematicBuilder builder(@NotNull Point size) {
        return new SizedSchematicBuilder(size);
    }

    static @NotNull SchematicBuilder builder() {
        return new UnboundedSchematicBuilder();
    }

    void metadata(@NotNull String key, @NotNull BinaryTag value);

    void block(@NotNull Point point, @NotNull Block block);
    default void block(int x, int y, int z, @NotNull Block block) {
        block(new Vec(x, y, z), block);
    }

    void offset(@NotNull Point point);
    default void offset(int x, int y, int z) {
        offset(new Vec(x, y, z));
    }

    @NotNull Schematic build();

}
