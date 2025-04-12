package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V3076 extends DataVersion {
    public V3076() {
        super(3076);

        addReference(DataType.BLOCK_ENTITY, "minecraft:sculk_catalyst");
    }
}
