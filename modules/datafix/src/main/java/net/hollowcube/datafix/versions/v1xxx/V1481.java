package net.hollowcube.datafix.versions.v1xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V1481 extends DataVersion {
    public V1481() {
        super(1481);

        addReference(DataTypes.BLOCK_ENTITY, "minecraft:conduit");
    }
}
