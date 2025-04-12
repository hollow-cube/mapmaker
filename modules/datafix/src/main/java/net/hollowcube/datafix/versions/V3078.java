package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V3078 extends DataVersion {
    public V3078() {
        super(3078);

        addReference(DataType.ENTITY, "minecraft:frog");
        addReference(DataType.ENTITY, "minecraft:tadpole");

        addReference(DataType.BLOCK_ENTITY, "minecraft:sculk_shrieker");
    }
}
