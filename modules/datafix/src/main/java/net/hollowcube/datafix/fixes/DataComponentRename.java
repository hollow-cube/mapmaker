package net.hollowcube.datafix.fixes;

import net.hollowcube.datafix.util.Value;

import java.util.function.Function;

public record DataComponentRename(
        String oldName,
        String newName,
        Function<Value, Value> transformer
) implements Function<Value, Value> {

    @Override
    public Value apply(Value dataComponents) {
        var oldComponent = dataComponents.remove(oldName);
        if (oldComponent.isNull()) return null;

        var newComponent = transformer.apply(oldComponent);
        if (newComponent != null) dataComponents.put(newName, newComponent);

        return null;
    }

}
