package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V3203 extends DataVersion {
    public V3203() {
        super(3203);

        addReference(DataTypes.ENTITY, "minecraft:camel");
    }
}
