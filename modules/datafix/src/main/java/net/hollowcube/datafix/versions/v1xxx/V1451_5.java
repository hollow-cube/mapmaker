package net.hollowcube.datafix.versions.v1xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V1451_5 extends DataVersion {
    public V1451_5() {
        super(1451); // todo what is this version?

        removeReference(DataTypes.BLOCK_ENTITY, "minecraft:flower_pot");
        removeReference(DataTypes.BLOCK_ENTITY, "minecraft:noteblock");

        // TODO: flattening
    }
}
