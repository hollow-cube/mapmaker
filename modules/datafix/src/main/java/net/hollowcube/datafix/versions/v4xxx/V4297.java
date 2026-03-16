package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.Nullable;

public class V4297 extends DataVersion {

    public V4297() {
        super(4297);

        addFix(DataTypes.ENTITY, "minecraft:area_effect_cloud", V4297::fixAreaEffectCloudPotionDurationScale);
    }

    private static @Nullable Value fixAreaEffectCloudPotionDurationScale(Value entity) {
        entity.put("potion_duration_scale", Value.wrap(0.25f));
        return null;
    }

}
