package net.hollowcube.datafix.fixes;

import net.hollowcube.datafix.DataFix;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public record DataComponentRename(
    String oldName,
    String newName,
    Function<Value, @Nullable Value> transformer
) implements DataFix {

    @Override
    public @Nullable Value fix(Value dataComponents) {
        var oldComponent = dataComponents.remove(oldName);
        if (oldComponent.isNull()) return null;

        var newComponent = transformer.apply(oldComponent);
        if (newComponent != null) dataComponents.put(newName, newComponent);

        return null;
    }

}
