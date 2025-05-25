package net.hollowcube.mapmaker.map.item.checkpoint;

import net.hollowcube.common.util.dfu.ExtraCodecs;
import net.hollowcube.mapmaker.map.action.ActionList;
import net.hollowcube.mapmaker.map.action.gui.ControlledNumberInput;
import net.hollowcube.mapmaker.map.action.impl.GiveItemAction;
import net.kyori.adventure.key.Key;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

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

    private static final ItemStack DEFAULT_ITEM = ItemStack.of(Material.ENDER_PEARL);

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
        // todo support infinite amount
        return DEFAULT_ITEM.withAmount(Math.max(1, this.amount));
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
