package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V3818_2 extends DataVersion {
    public V3818_2() {
        super(3818, 2);

        addFix(DataTypes.ENTITY, "minecraft:arrow", V3818_2::fixArrowEntityPotionFormat);
    }

    private static Value fixArrowEntityPotionFormat(Value entity) {
        var potion = entity.remove("Potion");
        var customPotionEffects = entity.remove("custom_potion_effects");
        var color = entity.remove("Color");
        if (potion.isNull() && customPotionEffects.isNull() && color.isNull())
            return null;

        var itemTag = entity.get("item", Value::emptyMap).get("tag", Value::emptyMap);
        itemTag.put("Potion", potion);
        itemTag.put("custom_potion_effects", customPotionEffects);
        itemTag.put("CustomPotionColor", color);

        return null;
    }

}
