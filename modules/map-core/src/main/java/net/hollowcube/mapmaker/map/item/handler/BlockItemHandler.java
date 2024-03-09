package net.hollowcube.mapmaker.map.item.handler;

import net.hollowcube.mapmaker.map.event.BlockItemPlaceEvent;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jglrxavpok.hephaistos.nbt.NBT;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;

import java.util.Objects;
import java.util.function.Supplier;

public class BlockItemHandler extends ItemHandler {
    public static final Tag<NBT> BLOCK_DATA = Tag.NBT("block_data").defaultValue(new NBTCompound());

    private final Block block;
    private final Material material;
    private final ItemUpdateFunc updateFunc;

    public BlockItemHandler(@NotNull Supplier<BlockHandler> blockHandler, @NotNull Block block) {
        this(blockHandler, block, null, null);
    }


    public BlockItemHandler(@NotNull Supplier<BlockHandler> blockHandler, @NotNull Block block, @NotNull ItemUpdateFunc updateFunc) {
        this(blockHandler, block, null, updateFunc);
    }

    public BlockItemHandler(@NotNull Supplier<BlockHandler> blockHandler, @NotNull Block block, @Nullable Material material, @Nullable ItemUpdateFunc updateFunc) {
        super(blockHandler.get().getNamespaceId().asString(), RIGHT_CLICK_BLOCK);
        this.block = block.withHandler(blockHandler.get());
        this.material = material != null ? material :
                Objects.requireNonNull(block.registry().material(), "Block has no material: " + block);
        this.updateFunc = updateFunc;
    }

    @Override
    public @NotNull Material material() {
        return material;
    }

    @Override
    protected void updateItemStack(ItemStack.@NotNull Builder builder, @NotNull TagHandler data) {
        if (updateFunc != null) updateFunc.updateItemStack(builder, data);
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        var instance = click.instance();

        var blockData = (NBTCompound) click.itemStack().meta().getTag(BLOCK_DATA);
        var block = this.block.withNbt(blockData);
        var event = new BlockItemPlaceEvent(click.player(), click.placePosition(), block);
        EventDispatcher.callCancellable(event, () -> {

            instance.placeBlock(new BlockHandler.PlayerPlacement(
                    block, instance, click.placePosition(),
                    click.player(), click.hand(), click.face(),
                    0f, 0f, 0f
            ));

        });
    }
}
