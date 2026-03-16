package net.hollowcube.datafix.versions.v0xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.Nullable;

public class V147 extends DataVersion {
    public V147() {
        super(147);

        addFix(DataTypes.ENTITY, "ArmorStand", V147::fixArmorStandSilent);
    }

    private static @Nullable Value fixArmorStandSilent(Value field) {
        if (field.get("Silent").as(Boolean.class, false) && !field.get("Marker").as(Boolean.class, false))
            field.put("Silent", null);
        return null;
    }
}
