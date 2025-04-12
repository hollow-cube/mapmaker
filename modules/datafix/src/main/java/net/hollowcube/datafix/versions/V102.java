package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V102 extends DataVersion {
    public V102() {
        super(102);

        addReference(DataType.ITEM_STACK, field -> field
                .single("id", DataType.ITEM_NAME));
    }
}
