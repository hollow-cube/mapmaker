package net.hollowcube.datafix.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class NullValue implements Value {

    NullValue() {
    }

    @Override
    public boolean isNull() {
        return true;
    }

    @Override
    public @Nullable Object value() {
        return null;
    }

    @Override
    public <T> T as(@NotNull Class<T> type, T defaultValue) {
        return defaultValue;
    }

    @Override
    public int size(int defaultValue) {
        return defaultValue;
    }

    @Override
    public void add(Object value) {
        // noop
    }

    @Override
    public void add(int index, Object value) {
        // noop
    }

    @Override
    public Value get(int index) {
        return this;
    }

    @Override
    public @NotNull Iterator<Value> iterator() {
        return Collections.emptyIterator();
    }

    @Override
    public boolean isMapLike() {
        return false;
    }

    @Override
    public @NotNull Value get(@NotNull String key) {
        return this;
    }

    @Override
    public Object getValue(@NotNull String key) {
        return null;
    }

    @Override
    public @NotNull Value get(@NotNull String key, Supplier<Value> defaultValue) {
        return this;
    }

    @Override
    public void put(@NotNull String key, Object value) {
        // noop
    }

    @Override
    public void forEachEntry(@NotNull BiConsumer<String, Value> consumer) {
        // noop
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof NullValue;
    }
}
