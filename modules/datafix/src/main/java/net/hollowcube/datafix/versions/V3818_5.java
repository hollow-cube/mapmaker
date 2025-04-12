package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V3818_5 extends DataVersion {
    public V3818_5() {
        super(3818); // todo what is id

        addReference(DataType.ITEM_STACK, field -> field
                .single("components", DataType.DATA_COMPONENTS));
    }

}
