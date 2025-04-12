package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V3818 extends DataVersion {
    public V3818() {
        super(3818);

        addReference(DataType.BLOCK_ENTITY, "minecraft:beehive", field -> field
                // todo
                .list("bees.entity_data", DataType.ENTITY_TREE));
    }

}
