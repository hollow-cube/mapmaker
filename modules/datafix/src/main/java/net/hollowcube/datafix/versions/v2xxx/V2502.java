package net.hollowcube.datafix.versions.v2xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V2502 extends DataVersion {
    public V2502() {
        super(2502);

        addReference(DataTypes.ENTITY, "minecraft:hoglin");
    }
}
