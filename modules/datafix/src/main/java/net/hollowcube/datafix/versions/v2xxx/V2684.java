package net.hollowcube.datafix.versions.v2xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V2684 extends DataVersion {
    public V2684() {
        super(2684);

        addReference(DataTypes.BLOCK_ENTITY, "minecraft:sculk_sensor");
    }
}
