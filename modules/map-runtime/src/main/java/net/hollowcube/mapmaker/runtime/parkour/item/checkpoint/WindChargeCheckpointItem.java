package net.hollowcube.mapmaker.runtime.parkour.item.checkpoint;

import net.hollowcube.common.util.dfu.ExtraCodecs;
import net.hollowcube.mapmaker.map.item.vanilla.WindChargeItem;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionList;
import net.hollowcube.mapmaker.runtime.parkour.action.gui.ControlledNumberInput;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.GiveItemAction;
import net.hollowcube.mapmaker.util.NumberUtil;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.component.UseCooldown;

import java.util.List;

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

    public WindChargeCheckpointItem withAmount(int amount) {
        return new WindChargeCheckpointItem(amount, this.cooldown);
    }

    public WindChargeCheckpointItem withCooldown(int cooldown) {
        return new WindChargeCheckpointItem(this.amount, cooldown);
    }

    @Override
    public ItemStack createItemStack() {
        return WindChargeItem.get(this.amount, this.cooldown > 0 ? new UseCooldown(this.cooldown / 20f, ID.asString()) : null);
    }

    @Override
    public CheckpointItem updateFromItemStack(ItemStack itemStack) {
        if (this.amount == INFINITE_AMOUNT) return this;
        return withAmount(itemStack.amount());
    }

    @Override
    public StructCodec<? extends CheckpointItem> codec() {
        return CODEC;
    }

    @Override
    public TranslatableComponent thumbnail() {
        return Component.translatable("gui.action.give_item.wind_charge.thumbnail", List.of(
                Component.text(this.amount == INFINITE_AMOUNT ? "Infinite" : String.valueOf(this.amount)),
                Component.text(this.cooldown > 0 ? NumberUtil.formatDuration(this.cooldown * 50L) : "No Cooldown")
        ));
    }

    @Override
    public GiveItemAction.AbstractItemEditor<?> createEditor(ActionList.Ref ref) {
        return new Editor(ref);
    }

    private static class Editor extends GiveItemAction.AbstractItemEditor<WindChargeCheckpointItem> {
        private final ControlledNumberInput amountInput;
        private final ControlledNumberInput cooldownInput;

        public Editor(ActionList.Ref ref) {
            super(ref, true);

            background("action/editor/container_lg", -10, -31);

            this.amountInput = add(1, 3, makeGenericAmount(WindChargeCheckpointItem::withAmount, MAX_AMOUNT));
            this.cooldownInput = add(1, 5, makeGenericCooldown(WindChargeCheckpointItem::withCooldown, true));
        }

        @Override
        protected void updateItem(WindChargeCheckpointItem item) {
            this.amountInput.update(item.amount);
            this.cooldownInput.update(item.cooldown);
        }
    }

}
