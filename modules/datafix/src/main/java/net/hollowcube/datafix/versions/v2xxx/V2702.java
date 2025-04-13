package net.hollowcube.datafix.versions.v2xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V2702 extends DataVersion {
    public V2702() {
        super(2702);

        addFix(DataType.ENTITY, "minecraft:arrow", V2702::updatePickup);
        addFix(DataType.ENTITY, "minecraft:spectral_arrow", V2702::updatePickup);
        addFix(DataType.ENTITY, "minecraft:trident", V2702::updatePickup);
    }

    private static Value updatePickup(Value entity) {
        if (entity.getValue("pickup") != null)
            return null;

        boolean player = entity.remove("player").as(Boolean.class, true);
        entity.put("pickup", (byte) (player ? 1 : 0));
        return null;
    }
}
