package net.hollowcube.datafix.versions.v2xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V2568 extends DataVersion {
    public V2568() {
        super(2568);

        addReference(DataTypes.ENTITY, "minecraft:piglin_brute");
    }
}
