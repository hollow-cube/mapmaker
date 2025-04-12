package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V1125 extends DataVersion {
    public V1125() {
        super(1125);

        addReference(DataType.BLOCK_ENTITY, "minecraft:bed");
    }
}
