package net.hollowcube.datafix.versions.v0xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V808 extends DataVersion {
    public V808() {
        super(808);

        addReference(DataType.BLOCK_ENTITY, "minecraft:shulker_box",
                field -> field.list("Items", DataType.ITEM_STACK));
    }
}
