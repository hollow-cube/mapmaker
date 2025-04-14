package net.hollowcube.datafix.versions.v1xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V1931 extends DataVersion {
    public V1931() {
        super(1931);

        addReference(DataTypes.ENTITY, "minecraft:fox");
    }
}
