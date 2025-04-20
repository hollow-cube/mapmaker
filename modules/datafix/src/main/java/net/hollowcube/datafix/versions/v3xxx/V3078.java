package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V3078 extends DataVersion {
    public V3078() {
        super(3078);

        addReference(DataTypes.ENTITY, "minecraft:frog");
        addReference(DataTypes.ENTITY, "minecraft:tadpole");

        addReference(DataTypes.BLOCK_ENTITY, "minecraft:sculk_shrieker");
    }
}
