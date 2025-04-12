package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V1904 extends DataVersion {
    public V1904() {
        super(1904);

        addReference(DataType.ENTITY, "minecraft:cat");
    }
}
