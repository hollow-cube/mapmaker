package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V1920 extends DataVersion {
    public V1920() {
        super(1920);

        addReference(DataType.BLOCK_ENTITY, "minecraft:campfire", field -> field
                .list("Items", DataType.ITEM_STA));
    }
}
