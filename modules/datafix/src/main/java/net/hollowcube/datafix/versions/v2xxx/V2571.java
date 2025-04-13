package net.hollowcube.datafix.versions.v2xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V2571 extends DataVersion {
    public V2571() {
        super(2571);

        addReference(DataType.ENTITY, "minecraft:goat");
    }
}
