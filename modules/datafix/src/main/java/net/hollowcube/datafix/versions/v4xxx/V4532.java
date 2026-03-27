package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V4532 extends DataVersion {

    public V4532() {
        super(4532);

        addReference(DataTypes.BLOCK_ENTITY, "minecraft:copper_golem_statue");
    }


}
