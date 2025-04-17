package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V3818_6 extends DataVersion {
    public V3818_6() {
        super(3818, 6);

        addFix(DataTypes.ENTITY, "minecraft:area_effect_cloud", V3818_6::fixAreaEffectCloud);
    }

    private static Value fixAreaEffectCloud(Value entity) {
        var color = entity.remove("Color");
        var effects = entity.remove("effects");
        var potion = entity.remove("Potion");
        if (color.isNull() && effects.isNull() && potion.isNull())
            return null;

        var potionContents = Value.emptyMap();
        if (!color.isNull()) potionContents.put("custom_color", color);
        if (!effects.isNull()) potionContents.put("custom_effects", effects);
        if (!potion.isNull()) potionContents.put("potion", potion);
        entity.put("potion_contents", potionContents);

        return null;
    }

}
