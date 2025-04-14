package net.hollowcube.datafix.versions.v1xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V1920 extends DataVersion {
    public V1920() {
        super(1920);

        addReference(DataTypes.BLOCK_ENTITY, "minecraft:campfire", field -> field
                .list("Items", DataTypes.ITEM_STACK));
    }
}
