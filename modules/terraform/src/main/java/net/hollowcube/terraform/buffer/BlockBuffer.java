package net.hollowcube.terraform.buffer;

import net.hollowcube.terraform.math.CoordinateUtil;
import net.hollowcube.terraform.schem.Schematic;
import net.hollowcube.terraform.schem.SchematicBuilder;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

/**
 * A BlockBuffer is a serializable set of section palettes that can be applied to a world.
 *
 * <p>BlockBuffer is the core type for block data being applied by a Terraform action.
 * It is used when computing the change set, storing the undo and redo data, etc.</p>
 */
public sealed interface BlockBuffer permits BlockBufferImpl {

    static @NotNull Builder builder() {
        return new NaiveBlockBufferBuilder();
    }

    /**
     * Creates a new builder with the given positions as the bounds of the buffer. The positions are inclusive,
     * so the buffer will contain all blocks between and including the two positions. The positions do not need
     * to be ordered.
     *
     * <p>A bounded builder is significantly more performant and should be preferred over an unbounded builder.</p>
     *
     * <p>An exception will be thrown if setting a block or palette outside of the bounds of the builder.</p>
     *
     * @param pos1 The first position (inclusive)
     * @param pos2 The second position (inclusive)
     * @return A new block buffer builder
     */
    static @NotNull Builder builder(@NotNull Point pos1, @NotNull Point pos2) {
        return new BoundedBlockBufferBuilder(pos1, pos2);
    }

    void forEachSection(@NotNull SectionConsumer consumer);

    long sizeBytes();

    default @NotNull Schematic toSchematic(@NotNull Point offset) {
        final var builder = new SchematicBuilder();
        builder.setOffset(CoordinateUtil.floor(offset));
        forEachSection((cx, cy, cz, palette) -> {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        var stateId = palette.get(x, y, z);
                        if (stateId != Palette.UNSET) {
                            var block = Block.fromStateId((short) stateId);
                            builder.addBlock(cx * 16 + x, cy * 16 + y, cz * 16 + z, block);
                        }
                    }
                }
            }
        });
        return builder.build();
    }

    @FunctionalInterface
    interface SectionConsumer {
        void accept(int chunkX, int chunkY, int chunkZ, @NotNull Palette palette);
    }

    /**
     * WARNING: Builder implementations are NOT thread safe.
     */
    sealed interface Builder permits BoundedBlockBufferBuilder, NaiveBlockBufferBuilder {

        /**
         * Set the block at the given (absolute) coordinate to the given value
         */
        void set(int x, int y, int z, int value);

        /**
         * Set the block at the given (absolute) coordinate to the given value
         */
        default void set(@NotNull Point point, int value) {
            set(point.blockX(), point.blockY(), point.blockZ(), value);
        }

        //todo does this break a bad boundary/limit the implementations in any way?
        //     for example if there is an adaptive palette being used, what do we do with the given palette? copy it?
        //     an alternative is a consumer for all positions, eg: `setSection(int chunkX, int chunkY, int chunkZ, @NotNull Function<XYZ, short>)
        // void setSection(int chunkX, int chunkY, int chunkZ, @NotNull Palette palette);

        @NotNull BlockBuffer build();

    }

}
