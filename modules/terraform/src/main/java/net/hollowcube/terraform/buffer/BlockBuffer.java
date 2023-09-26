package net.hollowcube.terraform.buffer;

import net.minestom.server.coordinate.Point;
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

    void forEachSection(@NotNull SectionConsumer consumer);


    @FunctionalInterface
    interface SectionConsumer {
        void accept(int chunkX, int chunkY, int chunkZ, @NotNull Palette palette);
    }

    /**
     * WARNING: Builder implementations are NOT thread safe.
     */
    sealed interface Builder permits NaiveBlockBufferBuilder {

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
        // void setSection(int chunkX, int chunkY, int chunkZ, @NotNull Palette palette);

        @NotNull BlockBuffer build();

    }

}
