package net.hollowcube.datafix.versions.v0xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V113 extends DataVersion {
    public V113() {
        super(113);

        addFix(DataType.ENTITY, V113::fixRedundantChanceTags);
    }

    private static Value fixRedundantChanceTags(Value entity) {
        if (isZeroList(entity.get("HandDropChances"), 2))
            entity.put("HandDropChances", null);
        if (isZeroList(entity.get("HandDropChances"), 4))
            entity.put("ArmorDropChances", null);
        return null;
    }

    private static boolean isZeroList(Value value, int length) {
        float[] array = value.as(float[].class, null);
        if (array == null || array.length != length) return false;
        for (float f : array) if (f != 0.0F) return false;
        return true;
    }
}
