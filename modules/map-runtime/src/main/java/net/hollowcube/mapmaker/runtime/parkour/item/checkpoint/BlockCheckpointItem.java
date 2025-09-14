package net.hollowcube.mapmaker.runtime.parkour.item.checkpoint;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.dfu.ExtraCodecs;
import net.hollowcube.mapmaker.panels.AnvilSearchView;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionList;
import net.hollowcube.mapmaker.runtime.parkour.action.gui.ControlledBlockListInput;
import net.hollowcube.mapmaker.runtime.parkour.action.gui.ControlledNumberInput;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.GiveItemAction;
import net.hollowcube.mapmaker.util.Autocompletors;
import net.hollowcube.mapmaker.util.ItemUtils;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.component.DataComponents;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.predicate.BlockPredicate;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.BlockPredicates;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.registry.RegistryTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record BlockCheckpointItem(Block block, int amount, List<Block> placeableOn) implements CheckpointItem {
    private static final int INFINITE_AMOUNT = 0;
    private static final int MAX_AMOUNT = 98;

    private static final List<Block> DEFAULT_PLACEABLE_ON = List.of(
            Block.STONE, Block.TARGET
    );

    public static final Key ID = Key.key("mapmaker:block");
    public static final StructCodec<BlockCheckpointItem> CODEC = StructCodec.struct(
            "block", ExtraCodecs.BLOCK_NAME_STRING.optional(Block.STONE), BlockCheckpointItem::block,
            "amount", Codec.INT.optional(INFINITE_AMOUNT), BlockCheckpointItem::amount,
            // we allow 8 here for historical reasons (we changed from always including the chosen block
            // to never including it, and a migration can result in 8 blocks in the list. The UI does not allow this.)
            "placeable_on", ExtraCodecs.BLOCK_NAME_STRING.list(8).optional(DEFAULT_PLACEABLE_ON), BlockCheckpointItem::placeableOn,
            BlockCheckpointItem::new);

    public BlockCheckpointItem withBlock(Block block) {
        return new BlockCheckpointItem(block, this.amount, List.of(block, Block.TARGET));
    }

    public BlockCheckpointItem withAmount(int amount) {
        return new BlockCheckpointItem(this.block, amount, this.placeableOn);
    }

    public BlockCheckpointItem withPlaceableOn(List<Block> placeableOn) {
        return new BlockCheckpointItem(this.block, this.amount, placeableOn);
    }

    public boolean isValid() {
        return ItemUtils.PLACEABLE_BLOCKS.contains(this.block()) && ItemUtils.PLACEABLE_ON_BLOCKS.containsAll(this.placeableOn());
    }

    @Override
    public StructCodec<? extends CheckpointItem> codec() {
        return CODEC;
    }

    @Override
    public ItemStack createItemStack() {
        if (!isValid()) return ItemStack.AIR;

        var canPlaceOn = new ArrayList<RegistryKey<Block>>(this.placeableOn);
        var material = Objects.requireNonNullElse(this.block.registry().material(), Material.STONE);
        int amount = this.amount == INFINITE_AMOUNT ? MAX_AMOUNT + 1 : this.amount;
        return ItemStack.of(material, amount).with(DataComponents.MAX_STACK_SIZE, amount)
                .with(DataComponents.CAN_PLACE_ON, new BlockPredicates(new BlockPredicate(RegistryTag.direct(canPlaceOn))));
    }

    @Override
    public CheckpointItem updateFromItemStack(ItemStack itemStack) {
        if (itemStack.amount() == MAX_AMOUNT + 1) return this;
        return withAmount(itemStack.amount());
    }

    @Override
    public TranslatableComponent thumbnail() {
        if (this.isValid()) {
            return Component.translatable("gui.action.give_item.block.thumbnail", List.of(
                    LanguageProviderV2.getVanillaTranslation(this.block),
                    Component.text(this.amount == INFINITE_AMOUNT ? "Infinite" : String.valueOf(this.amount)),
                    Component.text(this.placeableOn.size())
            ));
        }

        return Component.translatable("gui.action.give_item.block.thumbnail.invalid");
    }

    @Override
    public GiveItemAction.AbstractItemEditor<?> createEditor(ActionList.Ref ref) {
        return new Editor(ref);
    }

    private static class Editor extends GiveItemAction.AbstractItemEditor<BlockCheckpointItem> {
        private final ControlledNumberInput amountInput;
        private final ControlledBlockListInput placeableOnInput;

        public Editor(ActionList.Ref ref) {
            super(ref, true);

            background("action/editor/container_lg", -10, -31);

            this.slotInput.iconButton()
                    .onLeftClick(() -> {
                        host.pushView(new AnvilSearchView<>("action/anvil/search_icon", "Search Blocks",
                                Autocompletors::searchBlocks, ControlledBlockListInput::makeBlockButton, block -> {
                            updateItem(BlockCheckpointItem::withBlock).accept(block);
                        }));
                    });

            this.amountInput = add(1, 3, makeGenericAmount(BlockCheckpointItem::withAmount, MAX_AMOUNT));
            this.placeableOnInput = add(1, 5, new ControlledBlockListInput(7,
                    updateItem(BlockCheckpointItem::withPlaceableOn)));
        }

        @Override
        protected void updateItem(BlockCheckpointItem item) {
            this.slotInput.iconButton().model(item.block.name(), null);
            this.amountInput.update(item.amount);
            this.placeableOnInput.update(item.placeableOn);

            if (!item.isValid()) {
                this.slotInput.iconButton().text(
                        Component.translatable("gui.action.give_item.block.invalid.name"),
                        LanguageProviderV2.translateMulti("gui.action.give_item.block.invalid.lore", List.of())
                );
            }
        }
    }

}
