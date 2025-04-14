package net.hollowcube.datafix.versions.v0xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V501 extends DataVersion {
    public V501() {
        super(501);

        addReference(DataTypes.ENTITY, "PolarBear");
    }
}
