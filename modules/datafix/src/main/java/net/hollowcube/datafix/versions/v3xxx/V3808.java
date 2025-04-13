package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V3808 extends DataVersion {
    public V3808() {
        super(3808);

        addReference(DataType.ENTITY, "minecraft:horse", field -> field
                .single("SaddleItem", DataType.ITEM_STACK));
    }

}
