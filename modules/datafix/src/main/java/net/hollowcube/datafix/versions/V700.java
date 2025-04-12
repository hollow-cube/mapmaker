package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V700 extends DataVersion {
    public V700() {
        super(700);

        addReference(DataType.ENTITY, "ElderGuardian");
    }
}
