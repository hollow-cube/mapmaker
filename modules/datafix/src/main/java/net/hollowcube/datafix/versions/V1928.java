package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V1928 extends DataVersion {
    public V1928() {
        super(1928);

        removeReference(DataType.ENTITY, "minecraft:illager_beast");
        addReference(DataType.ENTITY, "minecraft:ravager");
    }
}
