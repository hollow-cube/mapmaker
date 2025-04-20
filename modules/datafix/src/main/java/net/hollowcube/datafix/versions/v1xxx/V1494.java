package net.hollowcube.datafix.versions.v1xxx;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V1494 extends DataVersion {
    private static final Int2ObjectMap<String> ENCHANTMENT_IDS = new Int2ObjectOpenHashMap<>();

    public V1494() {
        super(1494);

        addFix(DataTypes.ITEM_STACK, V1494::fixItemEnchantmentNames);
    }

    private static Value fixItemEnchantmentNames(Value value) {
        var tag = value.get("tag");
        if (!tag.isMapLike()) return null;

        Value ench = tag.remove("ench");
        if (!ench.isNull()) {
            tag.put("Enchantments", ench);

            for (Value enchantment : ench) {
                int id = enchantment.get("id").as(Number.class, 0).intValue();
                enchantment.put("id", ENCHANTMENT_IDS.getOrDefault(id, "null"));
            }
        }

        for (Value enchantment : tag.get("StoredEnchantments")) {
            int id = enchantment.get("id").as(Number.class, 0).intValue();
            enchantment.put("id", ENCHANTMENT_IDS.getOrDefault(id, "null"));
        }

        return null;
    }

    static {
        ENCHANTMENT_IDS.put(0, "minecraft:protection");
        ENCHANTMENT_IDS.put(1, "minecraft:fire_protection");
        ENCHANTMENT_IDS.put(2, "minecraft:feather_falling");
        ENCHANTMENT_IDS.put(3, "minecraft:blast_protection");
        ENCHANTMENT_IDS.put(4, "minecraft:projectile_protection");
        ENCHANTMENT_IDS.put(5, "minecraft:respiration");
        ENCHANTMENT_IDS.put(6, "minecraft:aqua_affinity");
        ENCHANTMENT_IDS.put(7, "minecraft:thorns");
        ENCHANTMENT_IDS.put(8, "minecraft:depth_strider");
        ENCHANTMENT_IDS.put(9, "minecraft:frost_walker");
        ENCHANTMENT_IDS.put(10, "minecraft:binding_curse");
        ENCHANTMENT_IDS.put(16, "minecraft:sharpness");
        ENCHANTMENT_IDS.put(17, "minecraft:smite");
        ENCHANTMENT_IDS.put(18, "minecraft:bane_of_arthropods");
        ENCHANTMENT_IDS.put(19, "minecraft:knockback");
        ENCHANTMENT_IDS.put(20, "minecraft:fire_aspect");
        ENCHANTMENT_IDS.put(21, "minecraft:looting");
        ENCHANTMENT_IDS.put(22, "minecraft:sweeping");
        ENCHANTMENT_IDS.put(32, "minecraft:efficiency");
        ENCHANTMENT_IDS.put(33, "minecraft:silk_touch");
        ENCHANTMENT_IDS.put(34, "minecraft:unbreaking");
        ENCHANTMENT_IDS.put(35, "minecraft:fortune");
        ENCHANTMENT_IDS.put(48, "minecraft:power");
        ENCHANTMENT_IDS.put(49, "minecraft:punch");
        ENCHANTMENT_IDS.put(50, "minecraft:flame");
        ENCHANTMENT_IDS.put(51, "minecraft:infinity");
        ENCHANTMENT_IDS.put(61, "minecraft:luck_of_the_sea");
        ENCHANTMENT_IDS.put(62, "minecraft:lure");
        ENCHANTMENT_IDS.put(65, "minecraft:loyalty");
        ENCHANTMENT_IDS.put(66, "minecraft:impaling");
        ENCHANTMENT_IDS.put(67, "minecraft:riptide");
        ENCHANTMENT_IDS.put(68, "minecraft:channeling");
        ENCHANTMENT_IDS.put(70, "minecraft:mending");
        ENCHANTMENT_IDS.put(71, "minecraft:vanishing_curse");
    }
}
