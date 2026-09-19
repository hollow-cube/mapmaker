package net.hollowcube.datafix.versions.v5xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V5000 extends DataVersion {

    public V5000() {
        super(5000);

        addReference(DataTypes.ENTITY, "minecraft:cushion");
    }

}
