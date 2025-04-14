package net.hollowcube.datafix.versions.v0xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V808 extends DataVersion {
    public V808() {
        super(808);

        addReference(DataTypes.BLOCK_ENTITY, "minecraft:shulker_box",
                field -> field.list("Items", DataTypes.ITEM_STACK));
    }
}
