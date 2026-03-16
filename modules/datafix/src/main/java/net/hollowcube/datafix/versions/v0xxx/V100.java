package net.hollowcube.datafix.versions.v0xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.Nullable;

public class V100 extends DataVersion {
    public V100() {
        super(100);

        // TODO: Should compare this to paper V100 again, not sure.

        addReference(DataTypes.ENTITY_EQUIPMENT, field -> field
            .list("ArmorItems", DataTypes.ITEM_STACK)
            .list("HandItems", DataTypes.ITEM_STACK)
            .single("body_armor_item", DataTypes.ITEM_STACK)
            .single("saddle", DataTypes.ITEM_STACK));

        addFix(DataTypes.ENTITY, V100::fixEntityDropChances);
        addFix(DataTypes.ENTITY_EQUIPMENT, V100::fixEntityEquipment);
    }

    private static @Nullable Value fixEntityDropChances(Value entity) {
        var dropChances = entity.remove("DropChances");
        if (dropChances.size(0) == 0) return null;

        var hand = entity.get("HandDropChances", Value::emptyList);
        if (hand.size(0) < 1) hand.put(dropChances.get(0).as(Number.class, 0f).floatValue());

        var armor = entity.get("ArmorDropChances", Value::emptyList);
        if (armor.size(0) < 1) armor.put(dropChances.get(1).as(Number.class, 0f).floatValue());
        if (armor.size(0) < 1) armor.put(dropChances.get(2).as(Number.class, 0f).floatValue());
        if (armor.size(0) < 1) armor.put(dropChances.get(3).as(Number.class, 0f).floatValue());
        if (armor.size(0) < 1) armor.put(dropChances.get(4).as(Number.class, 0f).floatValue());

        return null;
    }

    private static @Nullable Value fixEntityEquipment(Value entity) {
        var equipment = entity.remove("Equipment");
        if (equipment.size(0) == 0) return null;

        // TODO: is an empty map actually correct for offhand item? TODO check vanilla for this
        var handItems = Value.emptyList();
        handItems.put(equipment.get(0));
        handItems.put(Value.emptyMap());
        entity.put("HandItems", handItems);

        if (equipment.size(0) > 1) {
            var armorItems = Value.emptyList();
            for (int i = 1; i < Math.min(equipment.size(0), 5); i++) {
                armorItems.put(equipment.get(i));
            }
            entity.put("ArmorItems", armorItems);
        }

        return null;
    }

}
