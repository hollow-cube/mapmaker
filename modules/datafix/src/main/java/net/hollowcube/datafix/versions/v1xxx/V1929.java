package net.hollowcube.datafix.versions.v1xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V1929 extends DataVersion {
    public V1929() {
        super(1929);

        addReference(DataTypes.ENTITY, "minecraft:wandering_trader", field -> field
            .list("Inventory", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "minecraft:trader_llama", field -> field
            .list("Items", DataTypes.ITEM_STACK)
            .single("SaddleItem", DataTypes.ITEM_STACK)
            .single("DecorItem", DataTypes.ITEM_STACK));
    }
}
