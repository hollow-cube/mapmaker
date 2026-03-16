package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.Nullable;

public class V4649 extends DataVersion {

    public V4649() {
        super(4649);

        addFix(DataTypes.DATA_COMPONENTS, V4649::fixTridentAnimation);
    }

    private static @Nullable Value fixTridentAnimation(Value dataComponents) {
        var consumable = dataComponents.get("minecraft:consumable");
        if (!consumable.isMapLike()) return null;

        var animation = consumable.get("animation").as(String.class, "");
        consumable.put("animation", "spear".equals(animation) ? "trident" : animation);
        return null;
    }
}
