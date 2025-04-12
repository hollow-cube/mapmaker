package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V1451_5 extends DataVersion {
    public V1451_5() {
        super(1451); // todo what is this version?

        removeReference(DataType.BLOCK_ENTITY, "minecraft:flower_pot");
        removeReference(DataType.BLOCK_ENTITY, "minecraft:noteblock");
    }
}
