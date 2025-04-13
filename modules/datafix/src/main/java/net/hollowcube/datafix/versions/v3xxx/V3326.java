package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V3326 extends DataVersion {
    public V3326() {
        super(3326);

        addReference(DataType.ENTITY, "minecraft:sniffer");
    }
}
