package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V4303 extends DataVersion {
    public V4303() {
        super(4303);

        addFix(DataTypes.ENTITY, V4303::fixEntityFallDistanceFloatToDouble);
    }

    private static Value fixEntityFallDistanceFloatToDouble(Value entity) {
        var fallDistance = entity.remove("FallDistance").as(Number.class, 0f);
        entity.put("fall_distance", fallDistance.doubleValue());
        return null;
    }

}
