package net.hollowcube.mapmaker.map.item.checkpoint;

import net.hollowcube.common.util.dfu.ExtraCodecs;
import net.hollowcube.mapmaker.map.action.ActionList;
import net.hollowcube.mapmaker.map.action.gui.ControlledNumberInput;
import net.hollowcube.mapmaker.map.action.impl.GiveItemAction;
import net.hollowcube.mapmaker.map.item.vanilla.EnderPearlItem;
import net.hollowcube.mapmaker.util.NumberUtil;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.UseCooldown;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record EnderPearlCheckpointItem(int amount, int cooldown) implements CheckpointItem {
    private static final int INFINITE_AMOUNT = 0;
    private static final int MAX_AMOUNT = 99;
    private static final int MIN_COOLDOWN = 1;
    private static final int MAX_COOLDOWN = 60 * 60 * 20; // 1h in ticks
    private static final int DEFAULT_COOLDOWN = 20; // Vanilla default

    public static final Key ID = Key.key("ender_pearl");
    public static final StructCodec<EnderPearlCheckpointItem> CODEC = StructCodec.struct(
            "amount", ExtraCodecs.clamppedInt(INFINITE_AMOUNT, MAX_AMOUNT).optional(INFINITE_AMOUNT), EnderPearlCheckpointItem::amount,
            "cooldown", ExtraCodecs.clamppedInt(MIN_COOLDOWN, MAX_COOLDOWN).optional(DEFAULT_COOLDOWN), EnderPearlCheckpointItem::cooldown,
            EnderPearlCheckpointItem::new);

    private static final ItemStack DEFAULT_ITEM = ItemStack.of(Material.STICK)
            .with(DataComponents.ITEM_NAME, Material.ENDER_PEARL.prototype().get(DataComponents.ITEM_NAME));

    public @NotNull EnderPearlCheckpointItem withAmount(int amount) {
        return new EnderPearlCheckpointItem(amount, this.cooldown);
    }

    public @NotNull EnderPearlCheckpointItem withCooldown(int cooldown) {
        return new EnderPearlCheckpointItem(this.amount, cooldown);
    }

    @Override
    public @NotNull StructCodec<? extends CheckpointItem> codec() {
        return CODEC;
    }

    @Override
    public @NotNull ItemStack createItemStack() {
        var itemStack = this.cooldown > 0
                ? DEFAULT_ITEM.with(DataComponents.USE_COOLDOWN, new UseCooldown(this.cooldown / 20f, ID.asString()))
                : DEFAULT_ITEM.without(DataComponents.USE_COOLDOWN);
        return EnderPearlItem.withCount(itemStack, this.amount);
    }

    @Override
    public @NotNull CheckpointItem updateFromItemStack(@NotNull ItemStack itemStack) {
        if (this.amount == INFINITE_AMOUNT) return this;
        return withAmount(itemStack.amount());
    }

    @Override
    public @NotNull TranslatableComponent thumbnail() {
        return Component.translatable("gui.action.give_item.ender_pearl.thumbnail", List.of(
                Component.text(this.amount == INFINITE_AMOUNT ? "Infinite" : String.valueOf(this.amount)),
                Component.text(this.cooldown > 0 ? NumberUtil.formatDuration(this.cooldown * 50L) : "No Cooldown")
        ));
    }

    @Override
    public @NotNull GiveItemAction.AbstractItemEditor<?> createEditor(ActionList.@NotNull Ref ref) {
        return new Editor(ref);
    }

    private static class Editor extends GiveItemAction.AbstractItemEditor<EnderPearlCheckpointItem> {
        private final ControlledNumberInput amountInput;
        private final ControlledNumberInput cooldownInput;

        public Editor(ActionList.@NotNull Ref ref) {
            super(ref, true);

            background("action/editor/container_lg", -10, -31);

            this.amountInput = add(1, 3, makeGenericAmount(EnderPearlCheckpointItem::withAmount));
            this.cooldownInput = add(1, 5, makeGenericCooldown(EnderPearlCheckpointItem::withCooldown, true));
        }

        @Override
        protected void updateItem(@NotNull EnderPearlCheckpointItem item) {
            this.amountInput.update(item.amount);
            this.cooldownInput.update(item.cooldown);
        }
    }
}
