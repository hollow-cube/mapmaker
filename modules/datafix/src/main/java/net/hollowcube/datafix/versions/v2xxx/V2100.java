package net.hollowcube.datafix.versions.v2xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V2100 extends DataVersion {
    public V2100() {
        super(2100);

        addReference(DataType.ENTITY, "minecraft:bee");
        addReference(DataType.ENTITY, "minecraft:bee_stinger");

        // TODO not sure this is right
        addReference(DataType.BLOCK_ENTITY, "minecraft:beehive", field -> field
                .list("Bees.EntityData", DataType.ENTITY_TREE));
    }
}
