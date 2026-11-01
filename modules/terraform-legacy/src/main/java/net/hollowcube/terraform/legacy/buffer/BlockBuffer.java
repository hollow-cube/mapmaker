package net.hollowcube.terraform.legacy.buffer;

import net.hollowcube.terraform.legacy.buffer.palette.Palette;
import net.hollowcube.terraform.legacy.task.edit.WorldView;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A BlockBuffer is a serializable set of section palettes that can be applied to a world.
 *
 * <p>BlockBuffer is the core type for block data being applied by a Terraform action.
 * It is used when computing the change set, storing the undo and redo data, etc.</p>
 */
public interface BlockBuffer {

    static @NotNull BlockBuffer empty() {
        return EmptyBlockBuffer.INSTANCE;
    }

    static @NotNull Builder builder(@Nullable WorldView world) {
        return new EditBlockBufferBuilder(world);
    }

    /**
     * Creates a new builder with the given positions as the bounds of the buffer. The positions are inclusive,
     * so the buffer will contain all blocks between and including the two positions. The positions do not need
     * to be ordered.
     *
     * <p>An exception will be thrown if setting a block outside of the bounds of the builder.</p>
     *
     * @param pos1 The first position (inclusive)
     * @param pos2 The second position (inclusive)
     * @return A new block buffer builder
     */
    static @NotNull Builder builder(@Nullable WorldView world, @NotNull Point pos1, @NotNull Point pos2) {
        return new EditBlockBufferBuilder(world, pos1, pos2);
    }

    void forEachSection(@NotNull SectionConsumer consumer);

    long sizeBytes();

    @FunctionalInterface
    interface SectionConsumer {
        void accept(int chunkX, int chunkY, int chunkZ, @NotNull Palette palette);
    }

    /**
     * WARNING: Builder implementations are NOT thread safe.
     */
    sealed interface Builder permits EditBlockBufferBuilder {

        /**
         * Set the block at the given (absolute) coordinate to the given value
         */
        void set(int x, int y, int z, int value) throws InterruptedException;

        /**
         * Set the block at the given (absolute) coordinate to the given value
         */
        default void set(@NotNull Point point, int value) throws InterruptedException {
            set(point.blockX(), point.blockY(), point.blockZ(), value);
        }

        /**
         * Set the block at the given (absolute) coordinate to the given value
         */
        void set(int x, int y, int z, @NotNull Block value) throws InterruptedException;

        /**
         * Set the block at the given (absolute) coordinate to the given value
         */
        default void set(@NotNull Point point, @NotNull Block value) throws InterruptedException {
            set(point.blockX(), point.blockY(), point.blockZ(), value);
        }

        @NotNull BlockBuffer build() throws InterruptedException;

    }

}
