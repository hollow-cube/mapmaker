package net.hollowcube.datafix.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public interface Value {

    static Value emptyMap() {

    }

    static Value emptyList() {

    }

    static Value wrap(Object object) {

    }

    @Nullable Object value();

    <T> T as(@NotNull Class<T> type, T defaultValue);

    // ListLike

    void add(Object value);

    // MapLike

    boolean isMapLike();

    @NotNull Value get(@NotNull String key);

    Object getValue(@NotNull String key);

    @NotNull Value get(@NotNull String key, Supplier<Value> defaultValue);

    void put(@NotNull String key, Object value);
}
