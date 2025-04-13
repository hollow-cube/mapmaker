package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V3803 extends DataVersion {
    public V3803() {
        super(3803);

        addFix(DataType.ITEM_STACK, V3803::fixSweepingEdgeEnchantmentName);
    }

    private static Value fixSweepingEdgeEnchantmentName(Value value) {
        var tag = value.get("tag");
        if (!tag.isMapLike()) return null;

        fixEnchantmentList(value.get("Enchantments"));
        fixEnchantmentList(value.get("StoredEnchantments"));
        return null;
    }

    private static void fixEnchantmentList(Value enchantmentList) {
        for (var enchantment : enchantmentList) {
            if ("minecraft:sweeping".equals(enchantment.getValue("id"))) {
                enchantment.put("id", "minecraft:sweeping_edge");
            }
        }
    }
}
