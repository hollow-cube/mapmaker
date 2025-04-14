package net.hollowcube.datafix.versions.v2xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V2100 extends DataVersion {
    public V2100() {
        super(2100);

        addReference(DataTypes.ENTITY, "minecraft:bee");
        addReference(DataTypes.ENTITY, "minecraft:bee_stinger");

        // TODO not sure this is right
        addReference(DataTypes.BLOCK_ENTITY, "minecraft:beehive", field -> field
                .list("Bees.EntityData", DataTypes.ENTITY_TREE));
    }
}
