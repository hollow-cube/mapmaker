package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V4997 extends DataVersion {

    public V4997() {
        super(4997);

        addReference(DataTypes.ENTITY, "minecraft:poplar_boat");
        addReference(DataTypes.ENTITY, "minecraft:poplar_chest_boat", V4067::chestBoat);
    }

}
