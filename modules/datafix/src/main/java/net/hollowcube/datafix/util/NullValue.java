package net.hollowcube.datafix.util;

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
    public <T> T as(Class<T> type, T defaultValue) {
        return defaultValue;
    }

    @Override
    public int size(int defaultValue) {
        return defaultValue;
    }

    @Override
    public void put(Object value) {
        // noop
    }

    @Override
    public void put(int index, Object value) {
        // noop
    }

    @Override
    public Value get(int index) {
        return this;
    }

    @Override
    public Iterator<Value> iterator() {
        return Collections.emptyIterator();
    }

    @Override
    public boolean isMapLike() {
        return false;
    }

    @Override
    public Value get(String key) {
        return this;
    }

    @Override
    public @Nullable Object getValue(String key) {
        return null;
    }

    @Override
    public Value get(String key, Supplier<Value> defaultValue) {
        return this;
    }

    @Override
    public void put(String key, @Nullable Object value) {
        // noop
    }

    @Override
    public void forEachEntry(BiConsumer<String, Value> consumer) {
        // noop
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof NullValue;
    }
}
