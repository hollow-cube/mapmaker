package net.hollowcube.datafix.versions.v0xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V808 extends DataVersion {
    public V808() {
        super(808);

        addReference(DataTypes.BLOCK_ENTITY, "minecraft:shulker_box",
                field -> field.list("Items", DataTypes.ITEM_STACK));

        // Paper: fixShulkerColor flattened down from V808_1
        addFix(DataTypes.ENTITY, "minecraft:shulker", V808::fixShulkerColor);
    }

    private static Value fixShulkerColor(Value entity) {
        if (!(entity.getValue("Color") instanceof Number))
            entity.put("Color", (byte) 10);
        return entity;
    }
}
