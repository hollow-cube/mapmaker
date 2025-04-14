package net.hollowcube.datafix.util;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;

public record ListValue(List<Object> value) implements Value {

    @Override
    public <T> T as(@NotNull Class<T> type, T defaultValue) {
        return defaultValue;
    }

    @Override
    public boolean isListLike() {
        return true;
    }

    @Override
    public int size(int defaultValue) {
        return value.size();
    }

    @Override
    public void add(Object value) {
        this.value.add(value);
    }

    @Override
    public void add(int index, Object value) {
        if (index < 0) return;
        if (index >= this.value.size()) {
            this.value.add(value);
        } else {
            this.value.add(index, value);
        }
    }

    @Override
    public Value get(int index) {
        if (index < 0 || index >= this.value.size()) return NULL;
        return Value.wrap(this.value.get(index));
    }

    @Override
    public @NotNull Iterator<Value> iterator() {
        var delegate = this.value.iterator();
        return new Iterator<Value>() {
            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public Value next() {
                return Value.wrap(delegate.next());
            }
        };
    }

    @Override
    public boolean equals(Object obj) {
        return false;
    }
}
