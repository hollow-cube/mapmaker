package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V4306 extends DataVersion {
    public V4306() {
        super(4302);

        removeReference(DataTypes.ENTITY, "minecraft:potion");

        addReference(DataTypes.ENTITY, "minecraft:splash_potion", field -> field
                .single("Item", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "minecraft:lingering_potion", field -> field
                .single("Item", DataTypes.ITEM_STACK));

        addFix(DataTypes.ENTITY, "minecraft:potion", V4306::fixSplitPotionEntity);
    }

    private static Value fixSplitPotionEntity(Value entity) {
        var newId = "minecraft:lingering_potion".equals(entity.get("Item").getValue("id"))
                ? "minecraft:lingering_potion" : "minecraft:splash_potion";
        entity.put("id", newId);
        return null;
    }

}
