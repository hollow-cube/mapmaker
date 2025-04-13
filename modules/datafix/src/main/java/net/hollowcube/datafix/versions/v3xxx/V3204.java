package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V3204 extends DataVersion {
    public V3204() {
        super(3204);

        addReference(DataType.BLOCK_ENTITY, "minecraft:chiseled_bookshelf", field -> field
                .list("Items", DataType.ITEM_STACK));
    }
}
