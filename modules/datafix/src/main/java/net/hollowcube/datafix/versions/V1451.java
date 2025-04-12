package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V1451 extends DataVersion {
    public V1451() {
        super(1451);

        addReference(DataType.BLOCK_ENTITY, "minecraft:trapped_chest", field -> field
                .list("Items", DataType.ITEM_STACK));
    }
}
