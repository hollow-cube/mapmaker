package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V3327 extends DataVersion {
    public V3327() {
        super(3327);

        addReference(DataType.BLOCK_ENTITY, "minecraft:decorated_pot", field -> field
                .list("shards", DataType.ITEM_NAME)
                .single("item", DataType.ITEM_STACK));
        addReference(DataType.BLOCK_ENTITY, "minecraft:suspicious_sand", field -> field
                .single("item", DataType.ITEM_STACK));
    }
}
