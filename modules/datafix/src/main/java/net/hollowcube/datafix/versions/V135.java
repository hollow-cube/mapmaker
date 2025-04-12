package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V135 extends DataVersion {
    public V135() {
        super(135);

        addReference(DataType.ENTITY_TREE, field -> field
                .list("Passengers", DataType.ENTITY_TREE));
    }
}
