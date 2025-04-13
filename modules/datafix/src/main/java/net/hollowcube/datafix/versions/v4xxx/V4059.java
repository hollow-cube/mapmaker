package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V4059 extends DataVersion {
    public V4059() {
        super(4059);

        addReference(DataType.DATA_COMPONENTS, field -> field
                        // todo we should be removing food here
                        .single("minecraft:use_remainder", DataType.ITEM_STACK)
                // TODO equippable here.
        );
    }

}
