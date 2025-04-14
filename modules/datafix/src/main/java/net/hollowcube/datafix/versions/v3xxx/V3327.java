package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V3327 extends DataVersion {
    public V3327() {
        super(3327);

        addReference(DataTypes.BLOCK_ENTITY, "minecraft:decorated_pot", field -> field
                .list("shards", DataTypes.ITEM_NAME)
                .single("item", DataTypes.ITEM_STACK));
        addReference(DataTypes.BLOCK_ENTITY, "minecraft:suspicious_sand", field -> field
                .single("item", DataTypes.ITEM_STACK));
    }
}
