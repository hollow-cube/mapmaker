package net.hollowcube.datafix.versions.v2xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V2688 extends DataVersion {
    public V2688() {
        super(2688);

        addReference(DataTypes.ENTITY, "minecraft:glow_squid");

        addReference(DataTypes.ENTITY, "minecraft:glow_item_frame", field -> field
            .single("Item", DataTypes.ITEM_STACK));
    }
}
