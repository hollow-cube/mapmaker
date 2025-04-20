package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V4081 extends DataVersion {
    public V4081() {
        super(4081);

        addFix(DataTypes.ENTITY, "minecraft:salmon", V4081::fixSalmonType);
    }

    private static Value fixSalmonType(Value entity) {
        var type = entity.get("type").as(String.class, "medium");
        if ("large".equals(type)) entity.put("type", "medium");
        return null;
    }
}
