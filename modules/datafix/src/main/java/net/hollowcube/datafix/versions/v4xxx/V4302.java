package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V4302 extends DataVersion {
    public V4302() {
        super(4302);

        addReference(DataTypes.BLOCK_ENTITY, "minecraft:test_block");
        addReference(DataTypes.BLOCK_ENTITY, "minecraft:test_instance_block");
    }

}
