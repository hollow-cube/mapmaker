package net.hollowcube.datafix.versions.v1xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V1801 extends DataVersion {
    public V1801() {
        super(1801);

        addReference(DataTypes.ENTITY, "minecraft:illager_beast");
    }
}
