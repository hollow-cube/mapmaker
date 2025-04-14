package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V3818_5 extends DataVersion {
    public V3818_5() {
        super(3818); // todo what is id

        addReference(DataTypes.ITEM_STACK, field -> field
                .single("components", DataTypes.DATA_COMPONENTS));
    }

}
