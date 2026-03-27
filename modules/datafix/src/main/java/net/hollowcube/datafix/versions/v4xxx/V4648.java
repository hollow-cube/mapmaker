package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V4648 extends DataVersion {

    public V4648() {
        super(4648);

        addReference(DataTypes.ENTITY, "minecraft:nautilus");
        addReference(DataTypes.ENTITY, "minecraft:zombie_nautilus");
    }

}
