package net.hollowcube.mapmaker.map.item.handler;

import net.hollowcube.common.util.OpUtils;
import net.hollowcube.mapmaker.map.event.BlockItemPlaceEvent;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.component.CustomData;
import net.minestom.server.tag.TagHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class BlockItemHandler extends ItemHandler {

    private final Block block;
    private final BadSprite sprite;
    private final ItemUpdateFunc extraData;

    public BlockItemHandler(@NotNull Supplier<BlockHandler> blockHandler, @NotNull Block block, @NotNull String sprite) {
        this(blockHandler, block, sprite, null);
    }

    public BlockItemHandler(@NotNull Supplier<BlockHandler> blockHandler, @NotNull Block block, @NotNull String sprite, @Nullable ItemUpdateFunc extraData) {
        super(blockHandler.get().getKey().asString(), RIGHT_CLICK_BLOCK);
        this.block = block.withHandler(blockHandler.get());
        this.sprite = BadSprite.require(sprite);
        this.extraData = extraData;
    }

    public @NotNull Block block() {
        return block;
    }

    @Override
    public @Nullable BadSprite sprite() {
        return this.sprite;
    }

    @Override
    public void build(ItemStack.@NotNull Builder builder, @Nullable CompoundBinaryTag tag) {
        super.build(builder, tag);

        var material = this.block.registry().material();
        if (material != null) {
            builder.material(material);
        }
        if (this.extraData != null) {
            this.extraData.updateItemStack(
                    builder,
                    TagHandler.fromCompound(OpUtils.or(tag, CompoundBinaryTag::empty))
            );
        }
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        var instance = click.instance();

        var blockData = click.itemStack().get(DataComponents.BLOCK_ENTITY_DATA);
        var block = blockData != null ? this.block.withNbt(blockData.nbt()) : this.block;
        var existingBlock = instance.getBlock(click.blockPosition());
        var event = new BlockItemPlaceEvent(click.player(), new BlockVec(click.placePosition()), block);
        EventDispatcher.callCancellable(event, () -> {
            instance.placeBlock(new BlockHandler.PlayerPlacement(
                    block, existingBlock, instance, click.placePosition(),
                    click.player(), click.hand(), click.face(),
                    0f, 0f, 0f
            ));
        });
    }
}
