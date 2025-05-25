package net.hollowcube.mapmaker.map.item.checkpoint;

import net.hollowcube.common.util.dfu.ExtraCodecs;
import net.hollowcube.mapmaker.map.action.ActionList;
import net.hollowcube.mapmaker.map.action.gui.ControlledNumberInput;
import net.hollowcube.mapmaker.map.action.impl.GiveItemAction;
import net.hollowcube.mapmaker.map.item.vanilla.WindChargeItem;
import net.kyori.adventure.key.Key;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.UseCooldown;
import org.jetbrains.annotations.NotNull;

public record WindChargeCheckpointItem(int amount, int cooldown) implements CheckpointItem {
    private static final int INFINITE_AMOUNT = 0;
    private static final int MAX_AMOUNT = 99;
    private static final int MIN_COOLDOWN = 1;
    private static final int MAX_COOLDOWN = 60 * 60 * 20; // 1h in ticks
    private static final int DEFAULT_COOLDOWN = 10; // Vanilla default

    public static final Key ID = Key.key("wind_charge");
    public static final StructCodec<WindChargeCheckpointItem> CODEC = StructCodec.struct(
            "amount", ExtraCodecs.clamppedInt(INFINITE_AMOUNT, MAX_AMOUNT).optional(INFINITE_AMOUNT), WindChargeCheckpointItem::amount,
            "cooldown", ExtraCodecs.clamppedInt(MIN_COOLDOWN, MAX_COOLDOWN).optional(DEFAULT_COOLDOWN), WindChargeCheckpointItem::cooldown,
            WindChargeCheckpointItem::new);

    private static final ItemStack DEFAULT_ITEM = ItemStack.of(Material.STICK);

    public @NotNull WindChargeCheckpointItem withAmount(int amount) {
        return new WindChargeCheckpointItem(amount, this.cooldown);
    }

    public @NotNull WindChargeCheckpointItem withCooldown(int cooldown) {
        return new WindChargeCheckpointItem(this.amount, cooldown);
    }

    @Override
    public @NotNull ItemStack createItemStack() {
        var itemStack = this.cooldown > 0
                ? DEFAULT_ITEM.with(DataComponents.USE_COOLDOWN, new UseCooldown(this.cooldown / 20f, ID.asString()))
                : DEFAULT_ITEM.without(DataComponents.USE_COOLDOWN);
        return WindChargeItem.withCount(itemStack, this.amount);
    }

    @Override
    public @NotNull CheckpointItem updateFromItemStack(@NotNull ItemStack itemStack) {
        if (this.amount == INFINITE_AMOUNT) return this;
        return withAmount(itemStack.amount());
    }

    @Override
    public @NotNull StructCodec<? extends CheckpointItem> codec() {
        return CODEC;
    }

    @Override
    public @NotNull GiveItemAction.AbstractItemEditor<?> createEditor(ActionList.@NotNull Ref ref) {
        return new Editor(ref);
    }

    private static class Editor extends GiveItemAction.AbstractItemEditor<WindChargeCheckpointItem> {
        private final ControlledNumberInput amountInput;
        private final ControlledNumberInput cooldownInput;

        public Editor(ActionList.@NotNull Ref ref) {
            super(ref, true);

            background("action/editor/container_lg", -10, -31);

            this.amountInput = add(1, 3, makeGenericAmount(WindChargeCheckpointItem::withAmount));
            this.cooldownInput = add(1, 5, makeGenericCooldown(WindChargeCheckpointItem::withCooldown, true));
        }

        @Override
        protected void updateItem(@NotNull WindChargeCheckpointItem item) {
            this.amountInput.update(item.amount);
            this.cooldownInput.update(item.cooldown);
        }
    }

}
