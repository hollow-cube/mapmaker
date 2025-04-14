package net.hollowcube.datafix.versions.v2xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V2522 extends DataVersion {
    public V2522() {
        super(2522);

        addReference(DataTypes.ENTITY, "minecraft:zoglin");
    }
}
