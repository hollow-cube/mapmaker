package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.versions.v1xxx.V1458;

public class V3682 extends DataVersion {
    public V3682() {
        super(3682);

        addReference(DataType.BLOCK_ENTITY, "minecraft:crafter", V1458::nameableInventory);
    }
}
