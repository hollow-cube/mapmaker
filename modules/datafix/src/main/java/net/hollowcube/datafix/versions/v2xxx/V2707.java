package net.hollowcube.datafix.versions.v2xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V2707 extends DataVersion {
    public V2707() {
        super(2707);

        addReference(DataType.ENTITY, "minecraft:marker");
    }
}
