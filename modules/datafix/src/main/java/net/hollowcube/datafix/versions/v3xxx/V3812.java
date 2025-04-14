package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V3812 extends DataVersion {
    public V3812() {
        super(3218);

        addFix(DataTypes.ENTITY, "minecraft:wolf", V3812::fixWolfHealth);
    }

    private static Value fixWolfHealth(Value value) {
        boolean didUpdate = false;
        for (var attribute : value.get("Attributes")) {
            if (!"minecraft:generic.max_health".equals(attribute.getValue("Name")))
                continue;

            var baseHealth = attribute.get("Base").as(Number.class, 0.0).doubleValue();
            if (baseHealth == 20.0) attribute.put("Base", 40.0);
            didUpdate = true;
        }
        if (didUpdate) {
            value.put("Health", value.get("Health").as(Number.class, 0.0).floatValue() * 2.0F);
        }

        return null;
    }
}
