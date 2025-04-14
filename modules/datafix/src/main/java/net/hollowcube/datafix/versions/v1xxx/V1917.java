package net.hollowcube.datafix.versions.v1xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V1917 extends DataVersion {
    public V1917() {
        super(1917);

        addFix(DataTypes.ENTITY, "minecraft:cat", V1917::fixCatType);
    }

    private static Value fixCatType(Value value) {
        int catType = value.get("CatType").as(Number.class, 0).intValue();
        if (catType == 0) value.put("CatType", 10);
        return null;
    }
}
