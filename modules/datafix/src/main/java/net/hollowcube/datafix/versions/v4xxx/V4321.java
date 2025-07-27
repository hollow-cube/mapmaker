package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V4321 extends DataVersion {

    public V4321() {
        super(4321);

        addReference(DataTypes.ENTITY, "minecraft:happy_ghast");
    }


}
