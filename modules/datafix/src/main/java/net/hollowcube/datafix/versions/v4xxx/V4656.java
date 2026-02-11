package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V4656 extends DataVersion {

    public V4656() {
        super(4656);

        addReference(DataTypes.ENTITY, "minecraft:camel_husk");
        addReference(DataTypes.ENTITY, "minecraft:parched");
    }

}
