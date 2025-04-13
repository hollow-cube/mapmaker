package net.hollowcube.datafix.versions.v2xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V2533 extends DataVersion {
    private static final double OLD_BASE_VALUE = 16.0;
    private static final double NEW_BASE_VALUE = 48.0;

    public V2533() {
        super(2533);

        addFix(DataType.ENTITY, "minecraft:villager", V2533::fixVillagerFollowRange);
    }

    private static Value fixVillagerFollowRange(Value entity) {
        for (var attribute : entity.get("Attributes")) {
            if (!"generic.follow_range".equals(attribute.getValue("Name")))
                continue;
            double base = attribute.get("Base").as(Number.class, OLD_BASE_VALUE).doubleValue();
            if (base == OLD_BASE_VALUE) attribute.put("Base", NEW_BASE_VALUE);
        }
        return null;
    }
}
