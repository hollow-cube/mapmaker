package net.hollowcube.datafix.versions.v2xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V2704 extends DataVersion {
    public V2704() {
        super(2704);

        addReference(DataTypes.ENTITY, "minecraft:goat");
    }
}
