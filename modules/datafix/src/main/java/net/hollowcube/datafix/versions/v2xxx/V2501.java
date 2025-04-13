package net.hollowcube.datafix.versions.v2xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.versions.v1xxx.V1458;

public class V2501 extends DataVersion {
    public V2501() {
        super(2501);

        addReference(DataType.BLOCK_ENTITY, "minecraft:furnace", V1458::nameableInventory);
        addReference(DataType.BLOCK_ENTITY, "minecraft:smoker", V1458::nameableInventory);
        addReference(DataType.BLOCK_ENTITY, "minecraft:blast_furnace", V1458::nameableInventory);
    }
}
