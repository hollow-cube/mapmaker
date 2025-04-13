package net.hollowcube.datafix.versions.v2xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V2519 extends DataVersion {
    public V2519() {
        super(2519);

        addReference(DataType.ENTITY, "minecraft:strider");
    }
}
