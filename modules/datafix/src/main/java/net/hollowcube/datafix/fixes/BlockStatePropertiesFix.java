package net.hollowcube.datafix.fixes;

import net.hollowcube.datafix.DataFix;
import net.hollowcube.datafix.util.MapValue;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class BlockStatePropertiesFix implements DataFix {
    private final String id;
    private final Consumer<Value> function;

    public BlockStatePropertiesFix(@NotNull String id, @NotNull Consumer<Value> function) {
        this.id = id;
        this.function = function;
    }

    @Override
    public Value fix(Value blockState) {
        if (blockState.value() instanceof String s) {
            int index = s.indexOf('[');
            if (index == -1) return null;

            var name = s.substring(0, index);
            if (!this.id.equals(name)) return null;

            var states = s.substring(index + 1, s.length() - 1); // assume that it is a ]

            var map = Value.emptyMap();

            for (var string : states.split(",")) {
                var equals = string.split("=");
                map.put(equals[0], equals[1]);
            }

            function.accept(map);

            var builder = new StringBuilder();
            builder.append(name);

            if (map instanceof MapValue(Map<String, Object> mapValue) && !mapValue.isEmpty()) {
                builder.append("[");
                map.forEachEntry((property, value) -> {
                    builder.append(property)
                        .append('=')
                        .append(
                            Objects.requireNonNull(value.as(String.class, null))
                        ).append(",");
                });
                builder.deleteCharAt(builder.length() - 1);
                builder.append(']');
            }


            return Value.wrap(builder.toString());
        } else if (blockState.isMapLike()) {
            // Vanilla renamed Name/Properties to id/properties in 5006 (BlockStateFieldNamesFix)
            boolean renamed = blockState.getValue("id") instanceof String;
            if (!this.id.equals(blockState.getValue(renamed ? "id" : "Name"))) return null;

            var properties = blockState.get(renamed ? "properties" : "Properties");
            if (!properties.isMapLike() || properties.size(0) == 0) return null;

            function.accept(properties);
        }

        return null;
    }
}
