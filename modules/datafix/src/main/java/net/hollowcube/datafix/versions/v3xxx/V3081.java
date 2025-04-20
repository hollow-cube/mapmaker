package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V3081 extends DataVersion {
    public V3081() {
        super(3081);

        addReference(DataTypes.ENTITY, "minecraft:warden");
    }
}
