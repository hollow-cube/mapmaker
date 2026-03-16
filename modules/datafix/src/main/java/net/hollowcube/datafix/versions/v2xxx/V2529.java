package net.hollowcube.datafix.versions.v2xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.Nullable;

public class V2529 extends DataVersion {
    public V2529() {
        super(2529);

        addFix(DataTypes.ENTITY, "minecraft:strider", V2529::fixStriderGravity);
    }

    private static @Nullable Value fixStriderGravity(Value entity) {
        if (entity.get("NoGravity").as(Boolean.class, false))
            entity.put("NoGravity", false);
        return null;
    }
}
