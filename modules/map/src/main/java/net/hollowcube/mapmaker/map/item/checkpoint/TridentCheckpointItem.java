package net.hollowcube.mapmaker.map.item.checkpoint;

import net.hollowcube.common.util.dfu.ExtraCodecs;
import net.hollowcube.mapmaker.map.action.ActionList;
import net.hollowcube.mapmaker.map.action.gui.ControlledNumberInput;
import net.hollowcube.mapmaker.map.action.impl.GiveItemAction;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.EnchantmentList;
import net.minestom.server.item.enchant.Enchantment;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record TridentCheckpointItem(int riptide) implements CheckpointItem {
    public static final int MIN_RIPTIDE = 1;
    public static final int MAX_RIPTIDE = 3;

    public static final Key ID = Key.key("trident");
    public static final StructCodec<TridentCheckpointItem> CODEC = StructCodec.struct(
            "riptide", ExtraCodecs.clamppedInt(MIN_RIPTIDE, MAX_RIPTIDE).optional(MIN_RIPTIDE), TridentCheckpointItem::riptide,
            TridentCheckpointItem::new);

    private static final ItemStack DEFAULT_ITEM = ItemStack.of(Material.TRIDENT)
            .without(DataComponents.ATTRIBUTE_MODIFIERS);

    public TridentCheckpointItem withRiptide(int riptide) {
        return new TridentCheckpointItem(riptide);
    }

    @Override
    public @NotNull StructCodec<? extends CheckpointItem> codec() {
        return CODEC;
    }

    @Override
    public @NotNull ItemStack createItemStack() {
        return DEFAULT_ITEM.with(DataComponents.ENCHANTMENTS,
                EnchantmentList.EMPTY.with(Enchantment.RIPTIDE, riptide));
    }

    @Override
    public @NotNull TranslatableComponent thumbnail() {
        return Component.translatable("gui.action.give_item.trident.thumbnail", List.of(
                Component.text(this.riptide)
        ));
    }

    @Override
    public @NotNull GiveItemAction.AbstractItemEditor<?> createEditor(ActionList.@NotNull Ref ref) {
        return new Editor(ref);
    }

    private static class Editor extends GiveItemAction.AbstractItemEditor<TridentCheckpointItem> {
        private final ControlledNumberInput riptideInput;

        public Editor(ActionList.@NotNull Ref ref) {
            super(ref);

            this.riptideInput = add(1, 3, new ControlledNumberInput("give_item.trident.riptide",
                    updateItem(TridentCheckpointItem::withRiptide))
                    .range(MIN_RIPTIDE, MAX_RIPTIDE));
        }

        @Override
        protected void updateItem(@NotNull TridentCheckpointItem item) {
            this.riptideInput.update(item.riptide);
        }
    }
}
