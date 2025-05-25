package net.hollowcube.mapmaker.map.item.checkpoint;

import net.hollowcube.common.util.dfu.ExtraCodecs;
import net.hollowcube.mapmaker.map.action.ActionList;
import net.hollowcube.mapmaker.map.action.gui.ControlledNumberInput;
import net.hollowcube.mapmaker.map.action.impl.GiveItemAction;
import net.hollowcube.mapmaker.map.item.vanilla.FireworkRocketItem;
import net.hollowcube.mapmaker.util.NumberUtil;
import net.kyori.adventure.key.Key;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

// TODO supported item types:
// block (amount, placeable on - default target) todo need ∞ overlay?
public record FireworkRocketCheckpointItem(int amount, int duration) implements CheckpointItem {
    public static final int INFINITE_AMOUNT = 0;
    public static final int MAX_AMOUNT = 99;
    public static final int INFINITE_DURATION = 0;
    public static final int MAX_DURATION = 24 * 60 * 60 * 20; // 24h in ticks

    public static final Key ID = Key.key("firework_rocket");
    public static final StructCodec<FireworkRocketCheckpointItem> CODEC = StructCodec.struct(
            "amount", ExtraCodecs.clamppedInt(INFINITE_AMOUNT, MAX_AMOUNT).optional(INFINITE_AMOUNT), FireworkRocketCheckpointItem::amount,
            "duration", ExtraCodecs.clamppedInt(INFINITE_DURATION, MAX_DURATION).optional(INFINITE_DURATION), FireworkRocketCheckpointItem::duration,
            FireworkRocketCheckpointItem::new);

    private static final ItemStack DEFAULT_ITEM = ItemStack.of(Material.STICK);

    public @NotNull FireworkRocketCheckpointItem withAmount(int amount) {
        return new FireworkRocketCheckpointItem(amount, this.duration);
    }

    public @NotNull FireworkRocketCheckpointItem withDuration(int duration) {
        return new FireworkRocketCheckpointItem(this.amount, duration);
    }

    @Override
    public @NotNull StructCodec<? extends CheckpointItem> codec() {
        return CODEC;
    }

    @Override
    public @NotNull ItemStack createItemStack() {
        return FireworkRocketItem.setDurationMillis(
                FireworkRocketItem.withCount(DEFAULT_ITEM, this.amount),
                this.duration * 50);
    }

    @Override
    public @NotNull CheckpointItem updateFromItemStack(@NotNull ItemStack itemStack) {
        if (this.amount == INFINITE_AMOUNT) return this;
        return withAmount(itemStack.amount());
    }

    @Override
    public @NotNull GiveItemAction.AbstractItemEditor<?> createEditor(ActionList.@NotNull Ref ref) {
        return new Editor(ref);
    }

    private static class Editor extends GiveItemAction.AbstractItemEditor<FireworkRocketCheckpointItem> {
        private final ControlledNumberInput amountInput;
        private final ControlledNumberInput durationInput;

        public Editor(ActionList.@NotNull Ref ref) {
            super(ref, true);

            background("action/editor/container_lg", -10, -31);

            this.amountInput = add(1, 3, makeGenericAmount(FireworkRocketCheckpointItem::withAmount));

            this.durationInput = add(1, 5, new ControlledNumberInput("give_item.firework_rocket.duration",
                    updateItem(FireworkRocketCheckpointItem::withDuration), true)
                    .parsed(dur -> NumberUtil.formatDuration(dur * 50L), NumberUtil::parseDurationToTicks)
                    .formatted(dur -> dur == INFINITE_DURATION ? "Infinite" : NumberUtil.formatDuration(dur * 50L))
                    .range(INFINITE_DURATION, MAX_DURATION)
                    .stepped(20, 5 * 20)); // 1s, 5s steps
        }

        @Override
        protected void updateItem(@NotNull FireworkRocketCheckpointItem item) {
            this.amountInput.update(item.amount);
            this.durationInput.update(item.duration);
        }
    }
}
