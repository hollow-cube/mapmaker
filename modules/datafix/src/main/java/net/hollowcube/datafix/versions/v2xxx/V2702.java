package net.hollowcube.datafix.versions.v2xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.Nullable;

public class V2702 extends DataVersion {
    public V2702() {
        super(2702);

        addFix(DataTypes.ENTITY, "minecraft:arrow", V2702::updatePickup);
        addFix(DataTypes.ENTITY, "minecraft:spectral_arrow", V2702::updatePickup);
        addFix(DataTypes.ENTITY, "minecraft:trident", V2702::updatePickup);
    }

    private static @Nullable Value updatePickup(Value entity) {
        if (entity.getValue("pickup") != null)
            return null;

        boolean player = entity.remove("player").as(Boolean.class, true);
        entity.put("pickup", (byte) (player ? 1 : 0));
        return null;
    }
}
