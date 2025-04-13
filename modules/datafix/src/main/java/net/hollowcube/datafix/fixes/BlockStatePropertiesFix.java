package net.hollowcube.datafix.fixes;

import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Function;

public class BlockStatePropertiesFix implements Function<Value, Value> {
    private final String id;
    private final Consumer<Value> function;

    public BlockStatePropertiesFix(@NotNull String id, @NotNull Consumer<Value> function) {
        this.id = id;
        this.function = function;
    }

    @Override
    public Value apply(Value blockState) {
        if (blockState.getValue("Name") instanceof String s && this.id.equals(s)) {
            var properties = blockState.get("Properties");
            if (!properties.isMapLike() || properties.size(0) == 0) return null;

            function.accept(properties);
        }

        return null;
    }
}
