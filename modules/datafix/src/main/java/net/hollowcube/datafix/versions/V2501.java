package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V2501 extends DataVersion {
    public V2501() {
        super(2501);

        addReference(DataType.BLOCK_ENTITY, "minecraft:furnace", V1458::nameableInventory);
        addReference(DataType.BLOCK_ENTITY, "minecraft:smoker", V1458::nameableInventory);
        addReference(DataType.BLOCK_ENTITY, "minecraft:blast_furnace", V1458::nameableInventory);
    }
}
