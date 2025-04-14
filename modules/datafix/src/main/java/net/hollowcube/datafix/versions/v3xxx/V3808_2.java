package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V3808_2 extends DataVersion {
    public V3808_2() {
        super(3808, 2);

        addReference(DataTypes.ENTITY, "minecraft:trader_llama", field -> field
                .list("Items", DataTypes.ITEM_STACK)
                .single("SaddleItem", DataTypes.ITEM_STACK));

        addFix(DataTypes.ENTITY, "minecraft:trader_llama", entity ->
                V3808.fixHorseBodyArmorItem(entity, "DecorItem", false));
    }

}
