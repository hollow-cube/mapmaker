package net.hollowcube.datafix.versions.v2xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V2688 extends DataVersion {
    public V2688() {
        super(2688);

        addReference(DataType.ENTITY, "minecraft:glow_squid");

        addReference(DataType.ENTITY, "minecraft:glow_item_frame", field -> field
                .single("Item", DataType.ITEM_STACK));
    }
}
