package net.hollowcube.mapmaker.map.item.handler;

import net.hollowcube.mapmaker.map.event.BlockItemPlaceEvent;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.CustomData;
import net.minestom.server.tag.TagHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class BlockItemHandler extends ItemHandler {

    private final Block block;
    private final BadSprite sprite;
    private final ItemUpdateFunc updateFunc;

    public BlockItemHandler(@NotNull Supplier<BlockHandler> blockHandler, @NotNull Block block, @NotNull String sprite) {
        this(blockHandler, block, sprite, null);
    }

    public BlockItemHandler(@NotNull Supplier<BlockHandler> blockHandler, @NotNull Block block, @NotNull String sprite, @Nullable ItemUpdateFunc updateFunc) {
        super(blockHandler.get().getKey().asString(), RIGHT_CLICK_BLOCK);
        this.block = block.withHandler(blockHandler.get());
        this.sprite = BadSprite.require(sprite);
        this.updateFunc = updateFunc;
    }

    public @NotNull Block block() {
        return block;
    }

    @Override
    public @Nullable BadSprite sprite() {
        return this.sprite;
    }

    @Override
    public @Nullable Material material() {
        return block.registry().material();
    }

    @Override
    protected void updateItemStack(ItemStack.@NotNull Builder builder, @NotNull TagHandler data) {
        if (updateFunc != null) updateFunc.updateItemStack(builder, data);
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        var instance = click.instance();

        var blockData = click.itemStack().get(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);
        var block = this.block.withNbt(blockData.nbt());
        var event = new BlockItemPlaceEvent(click.player(), new BlockVec(click.placePosition()), block);
        EventDispatcher.callCancellable(event, () -> {
            instance.placeBlock(new BlockHandler.PlayerPlacement(
                    block, instance, click.placePosition(),
                    click.player(), click.hand(), click.face(),
                    0f, 0f, 0f
            ));
        });
    }
}
