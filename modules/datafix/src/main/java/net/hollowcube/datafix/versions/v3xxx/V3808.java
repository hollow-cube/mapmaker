package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.Nullable;

public class V3808 extends DataVersion {
    public V3808() {
        super(3808);

        addReference(DataTypes.ENTITY, "minecraft:horse", field -> field
            .single("SaddleItem", DataTypes.ITEM_STACK));

        addFix(DataTypes.ENTITY, "minecraft:horse", entity ->
            V3808.fixHorseBodyArmorItem(entity, "ArmorItem", true));
    }

    static @Nullable Value fixHorseBodyArmorItem(Value entity, String oldArmorName, boolean clearArmorItems) {
        var oldArmorItem = entity.remove(oldArmorName);
        if (!oldArmorItem.isMapLike()) return null;

        if (clearArmorItems) {
            entity.get("ArmorItems").put(2, Value.emptyMap());
            entity.get("ArmorDropChances").put(2, 0.85F);
        }

        oldArmorItem.put("body_armor_item", oldArmorItem);
        oldArmorItem.put("body_armor_drop_chance", 2.0F);
        return null;
    }

}
