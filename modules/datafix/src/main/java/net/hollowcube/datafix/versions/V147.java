package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V147 extends DataVersion {
    public V147() {
        super(147);

        addFix(DataType.ENTITY, "ArmorStand", V147::fixArmorStandSilent);
    }

    private static Value fixArmorStandSilent(Value field) {
        if (field.get("Silent").as(Boolean.class, false) && !field.get("Marker").as(Boolean.class, false))
            field.put("Silent", null);
        return null;
    }
}
