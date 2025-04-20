package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V4070 extends DataVersion {
    public V4070() {
        super(4070);

        addReference(DataTypes.ENTITY, "minecraft:pale_oak_boat");
        addReference(DataTypes.ENTITY, "minecraft:pale_oak_chest_boat", V4067::chestBoat);
    }

}
