package net.hollowcube.datafix.versions.v0xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V808_1 extends DataVersion {
    public V808_1() {
        super(808, 1);

        addFix(DataTypes.ENTITY, "minecraft:shulker", this::fixShulkerColor);
    }

    private Value fixShulkerColor(Value entity) {
        if (!(entity.getValue("Color") instanceof Number))
            entity.put("Color", (byte) 10);
        return entity;
    }
}
