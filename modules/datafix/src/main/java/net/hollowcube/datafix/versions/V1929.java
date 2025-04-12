package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V1929 extends DataVersion {
    public V1929() {
        super(1929);

        addReference(DataType.ENTITY, "minecraft:wandering_trader", field -> field
                .list("Inventory", DataType.ITEM_STACK));
        addReference(DataType.ENTITY, "minecraft:trader_llama", field -> field
                .list("Items", DataType.ITEM_STACK)
                .single("SaddleItem", DataType.ITEM_STACK)
                .single("DecorItem", DataType.ITEM_STACK));
    }
}
