package net.hollowcube.mapmaker.runtime.parkour.item.checkpoint;

import net.hollowcube.common.util.dfu.ExtraCodecs;
import net.hollowcube.mapmaker.map.item.vanilla.MaceItem;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionList;
import net.hollowcube.mapmaker.runtime.parkour.action.gui.ControlledNumberInput;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.GiveItemAction;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.item.ItemStack;

import java.util.List;

public record MaceCheckpointItem(int windBurst) implements CheckpointItem {
    public static final int MIN_WIND_BURST = 1;
    public static final int MAX_WIND_BURST = 25;

    public static final Key ID = Key.key("mace");
    public static final StructCodec<MaceCheckpointItem> CODEC = StructCodec.struct(
            "wind_burst", ExtraCodecs.clamppedInt(MIN_WIND_BURST, MAX_WIND_BURST).optional(MIN_WIND_BURST), MaceCheckpointItem::windBurst,
            MaceCheckpointItem::new);

    public MaceCheckpointItem withWindBurst(int windBurst) {
        return new MaceCheckpointItem(windBurst);
    }

    @Override
    public StructCodec<? extends CheckpointItem> codec() {
        return CODEC;
    }

    @Override
    public ItemStack createItemStack() {
        return MaceItem.get(this.windBurst);
    }

    @Override
    public TranslatableComponent thumbnail() {
        return Component.translatable("gui.action.give_item.mace.thumbnail", List.of(
                Component.text(this.windBurst)
        ));
    }

    @Override
    public GiveItemAction.AbstractItemEditor<?> createEditor(ActionList.Ref ref) {
        return new Editor(ref);
    }

    private static class Editor extends GiveItemAction.AbstractItemEditor<MaceCheckpointItem> {
        private final ControlledNumberInput windBurstInput;

        public Editor(ActionList.Ref ref) {
            super(ref);

            this.windBurstInput = add(1, 3, new ControlledNumberInput("give_item.mace.wind_burst",
                    updateItem(MaceCheckpointItem::withWindBurst))
                    .range(MIN_WIND_BURST, MAX_WIND_BURST));
        }

        @Override
        protected void updateItem(MaceCheckpointItem item) {
            this.windBurstInput.update(item.windBurst);
        }
    }
}
