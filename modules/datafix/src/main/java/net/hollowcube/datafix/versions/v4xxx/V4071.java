package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V4071 extends DataVersion {
    public V4071() {
        super(4071);

        addReference(DataTypes.ENTITY, "minecraft:creaking");
        addReference(DataTypes.ENTITY, "minecraft:creaking_transien");

        addReference(DataTypes.BLOCK_ENTITY, "minecraft:creaking_heart");
    }

}
