package net.hollowcube.mapmaker.map.item.checkpoint;

import net.hollowcube.mapmaker.map.action.ActionList;
import net.hollowcube.mapmaker.map.action.impl.GiveItemAction;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.component.DataComponents;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.predicate.BlockPredicate;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.BlockPredicates;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record BlockCheckpointItem() implements CheckpointItem {

    public static final Key ID = Key.key("mapmaker:block");
    public static final StructCodec<BlockCheckpointItem> CODEC = StructCodec.struct(
            BlockCheckpointItem::new);

    public @NotNull StructCodec<? extends CheckpointItem> codec() {
        return CODEC;
    }

    @Override
    public @NotNull ItemStack createItemStack() {
        return ItemStack.of(Material.STONE, 64)
                .with(DataComponents.CAN_PLACE_ON, new BlockPredicates(List.of(
                        new BlockPredicate(Block.STONE), // Itself
                        new BlockPredicate(Block.TARGET, Block.BEDROCK, Block.COBBLED_DEEPSLATE)
                )));
    }

    @Override
    public @NotNull TranslatableComponent thumbnail() {
        return Component.translatable("gui.action.give_item.block.thumbnail");
    }

    @Override
    public @NotNull GiveItemAction.AbstractItemEditor<?> createEditor(ActionList.@NotNull Ref ref) {
        return new Editor(ref);
    }

    private static class Editor extends GiveItemAction.AbstractItemEditor<BlockCheckpointItem> {

        public Editor(@NotNull ActionList.Ref ref) {
            super(ref, false);
        }

        @Override
        protected void updateItem(@NotNull BlockCheckpointItem item) {

        }
    }

}
