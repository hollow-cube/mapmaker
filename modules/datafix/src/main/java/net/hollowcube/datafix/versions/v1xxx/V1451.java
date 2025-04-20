package net.hollowcube.datafix.versions.v1xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V1451 extends DataVersion {
    public V1451() {
        super(1451);

        addReference(DataTypes.BLOCK_ENTITY, "minecraft:trapped_chest", field -> field
                .list("Items", DataTypes.ITEM_STACK));
    }
}
