package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V4881 extends DataVersion {

    public V4881() {
        super(4881);

        addReference(DataTypes.ENTITY, "minecraft:sulfur_cube");
        addReference(DataTypes.BLOCK_ENTITY, "minecraft:potent_sulfur");
    }

}
