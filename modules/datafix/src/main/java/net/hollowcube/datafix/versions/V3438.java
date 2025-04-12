package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V3438 extends DataVersion {
    public V3438() {
        super(3438);

        renameReference(DataType.BLOCK_ENTITY, "minecraft:suspicious_sand", "minecraft:brushable_block");
        addReference(DataType.BLOCK_ENTITY, "minecraft:calibrated_sculk_sensor");
    }
}
