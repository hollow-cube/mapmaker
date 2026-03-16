package net.hollowcube.datafix.util;

import com.google.gson.internal.LinkedTreeMap;
import net.minestom.server.codec.Transcoder;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public interface Value extends Iterable<Value> {
    Transcoder<Value> TRANSCODER = null; // todo

    Value NULL = new NullValue();

    static Value emptyMap() {
        return new MapValue(new HashMap<>());
    }

    static Value emptyList() {
        return new ListValue(new ArrayList<>());
    }

    static Value wrap(@Nullable Object object) {
        if (object == null) {
            return NULL;
        } else if (object instanceof Value v) {
            return v;
        } else if (object.getClass().isArray()) {
            return switch (object) {
                case byte[] arr -> new ByteArrayValue(arr);
                case int[] arr -> new IntArrayValue(arr);
                case long[] arr -> new LongArrayValue(arr);
                default -> throw new UnsupportedOperationException("unsupported array type: " + object.getClass());
            };
        } else if (object instanceof Map) {
            if (!(object instanceof HashMap<?, ?>) && !(object instanceof LinkedTreeMap<?, ?>))
                throw new UnsupportedOperationException("unexpected map implementation, cannot guarantee mutability: " + object.getClass());
            return new MapValue((Map<String, Object>) object);
        } else if (object instanceof List) {
            if (!(object instanceof ArrayList<?>))
                throw new UnsupportedOperationException("unexpected list implementation, cannot guarantee mutability: " + object.getClass());
            return new ListValue((List<Object>) object);
        } else {
            return new PrimitiveValue(object);
        }
    }

    default boolean isNull() {
        return false;
    }

    @Nullable Object value();

    <T> @UnknownNullability T as(Class<T> type, @UnknownNullability T defaultValue);

    // ListLike

    default boolean isListLike() {
        return false;
    }

    default int size(int defaultValue) {
        return defaultValue;
    }

    default void put(Object value) {
        // noop
    }

    default void put(int index, Object value) {
        // noop
    }

    default Value get(int index) {
        return NULL;
    }

    @Override
    default Iterator<Value> iterator() {
        return Collections.emptyIterator();
    }

    // MapLike

    default boolean isMapLike() {
        return false;
    }

    default Value get(String key) {
        return NULL;
    }

    // Remove and return
    default Value remove(String key) {
        var value = get(key);
        put(key, null);
        return value;
    }

    default @Nullable Object getValue(String key) {
        return null;
    }

    default Value get(String key, Supplier<Value> defaultValue) {
        return defaultValue.get();
    }

    default void put(String key, @Nullable Object value) {
        // noop
    }

    default void forEachEntry(BiConsumer<String, Value> consumer) {
        // noop
    }
}
