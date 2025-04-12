package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V4302 extends DataVersion {
    public V4302() {
        super(4302);

        addReference(DataType.BLOCK_ENTITY, "minecraft:test_block");
        addReference(DataType.BLOCK_ENTITY, "minecraft:test_instance_block");
    }

}
