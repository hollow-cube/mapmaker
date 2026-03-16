package net.hollowcube.datafix.versions.v1xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.Nullable;

public class V1125 extends DataVersion {
    public V1125() {
        super(1125);

        addReference(DataTypes.BLOCK_ENTITY, "minecraft:bed");

        addFix(DataTypes.ITEM_STACK, "minecraft:bed", V1125::fixBedItemColor);
    }

    private static @Nullable Value fixBedItemColor(Value value) {
        int damage = value.get("Damage").as(Number.class, 0).intValue();
        if (damage == 0) value.put("Damage", (short) 14);
        return null;
    }
}
