package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V3076 extends DataVersion {
    public V3076() {
        super(3076);

        addReference(DataTypes.BLOCK_ENTITY, "minecraft:sculk_catalyst");
    }
}
