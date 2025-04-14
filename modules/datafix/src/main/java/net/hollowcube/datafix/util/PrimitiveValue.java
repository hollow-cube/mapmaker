package net.hollowcube.datafix.util;

import org.jetbrains.annotations.NotNull;

public record PrimitiveValue(
        @NotNull Object value
) implements Value {

    @Override
    public <T> T as(@NotNull Class<T> type, T defaultValue) {
        if (type.isInstance(value)) {
            return type.cast(value);
        } else if (value instanceof Number number) {
            if (type == int.class || type == Integer.class) {
                return type.cast(number.intValue());
            } else if (type == long.class || type == Long.class) {
                return type.cast(number.longValue());
            } else if (type == double.class || type == Double.class) {
                return type.cast(number.doubleValue());
            } else if (type == float.class || type == Float.class) {
                return type.cast(number.floatValue());
            } else if (type == short.class || type == Short.class) {
                return type.cast(number.shortValue());
            } else if (type == byte.class || type == Byte.class) {
                return type.cast(number.byteValue());
            }
        }
        return defaultValue;
    }
}
