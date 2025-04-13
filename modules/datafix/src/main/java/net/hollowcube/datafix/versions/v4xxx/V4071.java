package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V4071 extends DataVersion {
    public V4071() {
        super(4071);

        addReference(DataType.ENTITY, "minecraft:creaking");
        addReference(DataType.ENTITY, "minecraft:creaking_transien");

        addReference(DataType.BLOCK_ENTITY, "minecraft:creaking_heart");
    }

}
