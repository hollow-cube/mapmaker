package net.hollowcube.datafix.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public interface Value extends Iterable<Value> {

    static Value emptyMap() {

    }

    static Value emptyList() {

    }

    static Value wrap(Object object) {

    }

    boolean isNull();

    @Nullable Object value();

    <T> T as(@NotNull Class<T> type, T defaultValue);

    // ListLike

    int size(int defaultValue);

    void add(Object value);

    @Override
    @NotNull Iterator<Value> iterator();

    // MapLike

    boolean isMapLike();

    @NotNull Value get(@NotNull String key);

    // Remove and return
    default @NotNull Value remove(@NotNull String key) {
        var value = get(key);
        put(key, null);
        return value;
    }

    Object getValue(@NotNull String key);

    @NotNull Value get(@NotNull String key, Supplier<Value> defaultValue);

    void put(@NotNull String key, Object value);

    void forEachEntry(@NotNull BiConsumer<String, Value> consumer);
}
