package net.hollowcube.datafix.versions.v5xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V5016 extends DataVersion {

    public V5016() {
        super(5016);

        addFix(DataTypes.ITEM_STACK, "minecraft:map", V5016::fixUnfilledBuriedTreasureMapName);
    }

    private static Value fixUnfilledBuriedTreasureMapName(Value itemStack) {
        var itemName = itemStack.get("components").get("minecraft:item_name");
        if ("filled_map.buried_treasure".equals(V5008.getPlainTranslationKey(itemName)))
            itemName.put("translate", "item.minecraft.buried_treasure_map");
        return null;
    }

}
