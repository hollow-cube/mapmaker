package net.hollowcube.datafix.versions.v2xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V2502 extends DataVersion {
    public V2502() {
        super(2502);

        addReference(DataType.ENTITY, "minecraft:hoglin");
    }
}
