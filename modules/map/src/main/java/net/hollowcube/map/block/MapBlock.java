package net.hollowcube.map.block;

import com.google.auto.service.AutoService;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import net.hollowcube.common.facet.Facet;
import net.minestom.server.ServerProcess;
import net.minestom.server.instance.Section;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface MapBlock {

    @NotNull String id();


    /** The {@link Block} for this block. Used when adding the block to the world. */
    @NotNull Block block();
    /** The {@link BlockHandler} for this block. Only used to register the handler with Minestom so that it may be loaded correctly. */
    @NotNull BlockHandler blockHandler();
    /** The block item for this block. CMD is compared to check if the block should be placed. */
    @NotNull ItemStack blockItem();

    /**
     * The GUI for configuring the options of this block
     * @return a 7x3 GUI, or null if this block has no options
     */
    @Nullable Section optionsGui();


    /**
     * Loads all {@link MapBlock}s from the classpath using SPI.
     */
    @SuppressWarnings("UnstableApiUsage")
    @AutoService(Facet.class)
    class Loader implements Facet {

        @Override
        public @NotNull ListenableFuture<Void> hook(@NotNull ServerProcess server) {
            return Futures.immediateVoidFuture();
        }

    }

}
