package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V3808 extends DataVersion {
    public V3808() {
        super(3808);

        addReference(DataType.ENTITY, "minecraft:horse", field -> field
                .single("SaddleItem", DataType.ITEM_STACK));

        addFix(DataType.ENTITY, "minecraft:horse", entity ->
                V3808.fixHorseBodyArmorItem(entity, "ArmorItem", true));
    }

    static Value fixHorseBodyArmorItem(Value entity, String oldArmorName, boolean clearArmorItems) {
        var oldArmorItem = entity.remove(oldArmorName);
        if (!oldArmorItem.isMapLike()) return null;

        if (clearArmorItems) {
            entity.get("ArmorItems").add(2, Value.emptyMap());
            entity.get("ArmorDropChances").add(2, 0.85F);
        }

        oldArmorItem.put("body_armor_item", oldArmorItem);
        oldArmorItem.put("body_armor_drop_chance", 2.0F);
        return null;
    }

}
