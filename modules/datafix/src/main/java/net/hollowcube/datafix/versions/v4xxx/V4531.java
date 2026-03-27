package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V4531 extends DataVersion {

    public V4531() {
        super(4531);

        addReference(DataTypes.ENTITY, "minecraft:copper_golem");
    }


}
