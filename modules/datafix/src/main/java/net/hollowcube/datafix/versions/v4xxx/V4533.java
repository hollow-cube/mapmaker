package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V4533 extends DataVersion {

    public V4533() {
        super(4533);

        addReference(DataTypes.BLOCK_ENTITY, "minecraft:shelf", field -> field
                .list("Items", DataTypes.ITEM_STACK));
    }


}
