package net.hollowcube.datafix.versions.v4xxx;

import it.unimi.dsi.fastutil.doubles.Double2DoubleFunction;
import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

import java.util.function.Function;

public class V4187 extends DataVersion {
    private static final String FOLLOW_RANGE = "minecraft:follow_range";

    public V4187() {
        super(4187);

        addFix(DataType.ENTITY, "minecraft:villager", fixFollowRangeAttributeBaseValue(d -> d == 48.0 ? 16.0 : d));
        addFix(DataType.ENTITY, "minecraft:bee", fixFollowRangeAttributeBaseValue(d -> d == 48.0 ? 16.0 : d));
        addFix(DataType.ENTITY, "minecraft:allay", fixFollowRangeAttributeBaseValue(d -> d == 48.0 ? 16.0 : d));
        addFix(DataType.ENTITY, "minecraft:llama", fixFollowRangeAttributeBaseValue(d -> d == 40.0 ? 16.0 : d));
        addFix(DataType.ENTITY, "minecraft:piglin_brute", fixFollowRangeAttributeBaseValue(d -> d == 16.0 ? 12.0 : d));
        addFix(DataType.ENTITY, "minecraft:warden", fixFollowRangeAttributeBaseValue(d -> d == 16.0 ? 24.0 : d));
    }

    private static Function<Value, Value> fixFollowRangeAttributeBaseValue(Double2DoubleFunction operator) {
        return entity -> {
            for (var attribute : entity.get("attributes")) {
                if (!FOLLOW_RANGE.equals(attribute.getValue("id")))
                    continue;

                var base = attribute.get("base").as(Number.class, 0.0d).doubleValue();
                attribute.put("base", operator.applyAsDouble(base));
            }
            return null;
        };
    }
}
