package net.hollowcube.mapmaker.runtime.parkour.action;

import net.hollowcube.mapmaker.runtime.parkour.item.checkpoint.CheckpointItem;
import net.hollowcube.mapmaker.runtime.parkour.item.checkpoint.CheckpointItems;
import net.minestom.server.codec.StructCodec;
import org.jetbrains.annotations.Nullable;

public record HotbarItems(
        @Nullable CheckpointItem item0,
        @Nullable CheckpointItem item1,
        @Nullable CheckpointItem item2
) {
    public static final HotbarItems EMPTY = new HotbarItems(null, null, null);

    public static final StructCodec<HotbarItems> CODEC = StructCodec.struct(
            "item0", CheckpointItems.CODEC.optional(), HotbarItems::item0,
            "item1", CheckpointItems.CODEC.optional(), HotbarItems::item1,
            "item2", CheckpointItems.CODEC.optional(), HotbarItems::item2,
            HotbarItems::new);

    public HotbarItems withItem(int index, @Nullable CheckpointItem item) {
        if (index == 0) {
            return new HotbarItems(item, item1, item2);
        } else if (index == 1) {
            return new HotbarItems(item0, item, item2);
        } else if (index == 2) {
            return new HotbarItems(item0, item1, item);
        } else {
            return this;
        }
    }

}
