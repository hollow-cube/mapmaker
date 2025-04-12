package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V3203 extends DataVersion {
    public V3203() {
        super(3203);

        addReference(DataType.ENTITY, "minecraft:camel");
    }
}
