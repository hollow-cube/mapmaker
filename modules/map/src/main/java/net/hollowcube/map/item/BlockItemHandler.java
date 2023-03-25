package net.hollowcube.map.item;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class BlockItemHandler extends ItemHandler {
    private final Block block;
    private final Material material;

    public BlockItemHandler(@NotNull BlockHandler blockHandler, @NotNull Block block) {
        this(blockHandler, block, null);
    }

    public BlockItemHandler(@NotNull BlockHandler blockHandler, @NotNull Block block, @Nullable Material material) {
        super(blockHandler.getNamespaceId().asString(), RIGHT_CLICK_BLOCK);
        this.block = block.withHandler(blockHandler);
        this.material = material != null ? material :
                Objects.requireNonNull(block.registry().material(), "Block has no material: " + block);
    }

    @Override
    public @NotNull Material material() {
        return material;
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        var instance = click.instance();

        instance.setBlock(click.placePosition(), block);
    }
}
