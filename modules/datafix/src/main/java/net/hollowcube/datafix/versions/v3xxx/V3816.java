package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V3816 extends DataVersion {
    public V3816() {
        super(3816);

        addReference(DataTypes.ENTITY, "minecraft:bogged");
    }

}
