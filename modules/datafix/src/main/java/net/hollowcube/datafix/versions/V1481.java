package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V1481 extends DataVersion {
    public V1481() {
        super(1481);

        addReference(DataType.BLOCK_ENTITY, "minecraft:conduit");
    }
}
