package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V3808_1 extends DataVersion {
    public V3808_1() {
        super(3808, 1);

        addReference(DataTypes.ENTITY, "minecraft:llama", field -> field
                .list("Items", DataTypes.ITEM_STACK)
                .single("SaddleItem", DataTypes.ITEM_STACK));

        addFix(DataTypes.ENTITY, "minecraft:llama", entity ->
                V3808.fixHorseBodyArmorItem(entity, "DecorItem", false));
    }

}
