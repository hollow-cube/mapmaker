package net.hollowcube.datafix.versions.v2xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V2529 extends DataVersion {
    public V2529() {
        super(2529);

        addFix(DataType.ENTITY, "minecraft:strider", V2529::fixStriderGravity);
    }

    private static Value fixStriderGravity(Value entity) {
        if (entity.get("NoGravity").as(Boolean.class, false))
            entity.put("NoGravity", false);
        return null;
    }
}
