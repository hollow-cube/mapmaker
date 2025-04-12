package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V1801 extends DataVersion {
    public V1801() {
        super(1801);

        addReference(DataType.ENTITY, "minecraft:illager_beast");
    }
}
