package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V3808_2 extends DataVersion {
    public V3808_2() {
        super(3808); // TODO what is id

        addReference(DataType.ENTITY, "minecraft:trader_llama", field -> field
                .list("Items", DataType.ITEM_STACK)
                .single("SaddleItem", DataType.ITEM_STACK));
    }

}
