package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V501 extends DataVersion {
    public V501() {
        super(501);

        addReference(DataType.ENTITY, "PolarBear");
    }
}
