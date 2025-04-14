package net.hollowcube.datafix.versions.v2xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V2519 extends DataVersion {
    public V2519() {
        super(2519);

        addReference(DataTypes.ENTITY, "minecraft:strider");
    }
}
