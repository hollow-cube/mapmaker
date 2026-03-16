package net.hollowcube.datafix.versions.v0xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.Nullable;

public class V806 extends DataVersion {
    public V806() {
        super(806);

        addFix(DataTypes.ITEM_STACK, "minecraft:potion", V806::fixWaterPotionItem);
        addFix(DataTypes.ITEM_STACK, "minecraft:splash_potion", V806::fixWaterPotionItem);
        addFix(DataTypes.ITEM_STACK, "minecraft:lingering_potion", V806::fixWaterPotionItem);
        addFix(DataTypes.ITEM_STACK, "minecraft:tipped_arrow", V806::fixWaterPotionItem);
    }

    private static @Nullable Value fixWaterPotionItem(Value value) {
        Value tag = value.get("tag");
        if (tag.value() == null) return null;

        if (value.getValue("Potion") == null)
            value.put("Potion", "minecraft:water");

        return null;
    }
}
