package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V701 extends DataVersion {
    public V701() {
        super(701);

        addReference(DataType.ENTITY, "WitherSkeleton");
        addReference(DataType.ENTITY, "Stray");
    }
}
