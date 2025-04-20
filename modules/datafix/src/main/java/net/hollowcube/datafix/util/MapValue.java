package net.hollowcube.datafix.util;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public record MapValue(
        // Sometimes these are Value instances, other times not.
        @NotNull Map<String, Object> value
) implements Value {

    @Override
    public <T> T as(@NotNull Class<T> type, T defaultValue) {
        return defaultValue;
    }

    @Override
    public int size(int defaultValue) {
        return value.size();
    }

    @Override
    public boolean isMapLike() {
        return true;
    }

    @Override
    public @NotNull Value get(@NotNull String key) {
        var result = value.get(key);
        if (result == null) return NULL;
        return Value.wrap(result);
    }

    @Override
    public Object getValue(@NotNull String key) {
        var result = value.get(key);
        return result instanceof Value v ? v.value() : result;
    }

    @Override
    public @NotNull Value get(@NotNull String key, Supplier<Value> defaultValue) {
        var result = value.get(key);
        if (result == null) {
            var newValue = defaultValue.get();
            put(key, newValue);
            return newValue;
        }
        return Value.wrap(result);
    }

    @Override
    public void put(@NotNull String key, Object value) {
        if (value == null || (value instanceof Value v && v.isNull())) {
            this.value.remove(key);
        } else {
            this.value.put(key, Value.wrap(value));
        }
    }

    @Override
    public void forEachEntry(@NotNull BiConsumer<String, Value> consumer) {
        this.value.forEach((key, value) ->
                consumer.accept(key, Value.wrap(value)));
    }
}
