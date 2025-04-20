package net.hollowcube.datafix.util;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public record IntArrayValue(int[] value) implements Value {
    @Override
    public <T> T as(@NotNull Class<T> type, T defaultValue) {
        return type.equals(value.getClass()) ? (T) value : defaultValue;
    }

    @Override
    public boolean isListLike() {
        return true;
    }

    @Override
    public int size(int defaultValue) {
        return value.length;
    }

    @Override
    public void put(int index, Object value) {
        if (index < 0 || index > this.value.length - 1)
            return;
        if (!(value instanceof Number n))
            return;
        this.value[index] = n.intValue();
    }

    @Override
    public @NotNull Iterator<Value> iterator() {
        throw new UnsupportedOperationException("int array iteration");
    }
}
