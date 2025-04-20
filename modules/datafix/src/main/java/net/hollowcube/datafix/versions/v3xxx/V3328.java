package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V3328 extends DataVersion {
    public V3328() {
        super(3328);

        addReference(DataTypes.ENTITY, "minecraft:interaction");
    }
}
