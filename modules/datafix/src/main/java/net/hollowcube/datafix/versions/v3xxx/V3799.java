package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V3799 extends DataVersion {
    public V3799() {
        super(3799);

        addReference(DataTypes.ENTITY, "minecraft:armadillo");
    }

}
