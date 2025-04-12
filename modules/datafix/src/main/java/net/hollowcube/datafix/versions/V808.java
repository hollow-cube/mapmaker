package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

import java.util.Map;

public class V808 extends DataVersion {
    public V808() {
        super(808);

        addReference(DataType.BLOCK_ENTITY, "minecraft:shulker_box",
                field -> field.list("Items", DataType.ITEM_STACK));
        addFix(DataType.ENTITY, "minecraft:shulker", this::fixShulkerColor);
    }

    private Map<String, Object> fixShulkerColor(Map<String, Object> entity) {
        if (!(entity.get("Color") instanceof Number))
            entity.put("Color", (byte) 10);
        return entity;
    }
}
