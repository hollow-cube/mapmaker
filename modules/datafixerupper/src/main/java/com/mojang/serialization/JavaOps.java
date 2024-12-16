package com.mojang.serialization;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * Ops for pure Java types.
 * This class MUST NOT discard any information (other than exact compound types) - there should be no data loss between 'create' and 'get' pairs.
 */
public class JavaOps implements DynamicOps<Object> {
    public static final JavaOps INSTANCE = new JavaOps();

    private JavaOps() {
    }

    @Override
    public Object empty() {
        return null;
    }

    @Override
    public Object emptyMap() {
        return Map.of();
    }

    @Override
    public Object emptyList() {
        return List.of();
    }

    @Override
    public <U> U convertTo(final DynamicOps<U> outOps, final Object input) {
        if (input == null) {
            return outOps.empty();
        }
        if (input instanceof Map) {
            return convertMap(outOps, input);
        }
        if (input instanceof final ByteList value) {
            return outOps.createByteList(ByteBuffer.wrap(value.toByteArray()));
        }
        if (input instanceof final IntList value) {
            return outOps.createIntList(value.intStream());
        }
        if (input instanceof final LongList value) {
            return outOps.createLongList(value.longStream());
        }
        if (input instanceof List) {
            return convertList(outOps, input);
        }
        if (input instanceof final String value) {
            return outOps.createString(value);
        }
        if (input instanceof final Boolean value) {
            return outOps.createBoolean(value);
        }
        if (input instanceof final Byte value) {
            return outOps.createByte(value);
        }
        if (input instanceof final Short value) {
            return outOps.createShort(value);
        }
        if (input instanceof final Integer value) {
            return outOps.createInt(value);
        }
        if (input instanceof final Long value) {
            return outOps.createLong(value);
        }
        if (input instanceof final Float value) {
            return outOps.createFloat(value);
        }
        if (input instanceof final Double value) {
            return outOps.createDouble(value);
        }
        if (input instanceof final Number value) {
            return outOps.createNumeric(value);
        }
        throw new IllegalStateException("Don't know how to convert " + input);
    }

    @Override
    public DataResult<Number> getNumberValue(final Object input) {
        if (input instanceof final Number value) {
            return DataResult.success(value);
        }
        return DataResult.error(() -> "Not a number: " + input);
    }

    @Override
    public Object createNumeric(final Number value) {
        return value;
    }

    @Override
    public Object createByte(final byte value) {
        return value;
    }

    @Override
    public Object createShort(final short value) {
        return value;
    }

    @Override
    public Object createInt(final int value) {
        return value;
    }

    @Override
    public Object createLong(final long value) {
        return value;
    }

    @Override
    public Object createFloat(final float value) {
        return value;
    }

    @Override
    public Object createDouble(final double value) {
        return value;
    }

    @Override
    public DataResult<Boolean> getBooleanValue(final Object input) {
        if (input instanceof final Boolean value) {
            return DataResult.success(value);
        }
        return DataResult.error(() -> "Not a boolean: " + input);
    }

    @Override
    public Object createBoolean(final boolean value) {
        return value;
    }

    @Override
    public DataResult<String> getStringValue(final Object input) {
        if (input instanceof final String value) {
            return DataResult.success(value);
        }
        return DataResult.error(() -> "Not a string: " + input);
    }

    @Override
    public Object createString(final String value) {
        return value;
    }

    @Override
    public DataResult<Object> mergeToList(final Object input, final Object value) {
        if (input == empty()) {
            return DataResult.success(List.of(value));
        }
        if (input instanceof final List<?> list) {
            if (list.isEmpty()) {
                return DataResult.success(List.of(value));
            }
            var builder = new ArrayList<>(list.size() + 1);
            builder.addAll(list);
            builder.add(value);
            return DataResult.success(List.copyOf(builder));
        }
        return DataResult.error(() -> "Not a list: " + input);
    }

    @Override
    public DataResult<Object> mergeToList(final Object input, final List<Object> values) {
        if (input == empty()) {
            return DataResult.success(values);
        }
        if (input instanceof final List<?> list) {
            if (list.isEmpty()) {
                return DataResult.success(values);
            }
            var builder = new ArrayList<>(list.size() + values.size());
            builder.addAll(list);
            builder.addAll(values);
            return DataResult.success(List.copyOf(builder));
        }
        return DataResult.error(() -> "Not a list: " + input);
    }

    @Override
    public DataResult<Object> mergeToMap(final Object input, final Object key, final Object value) {
        if (input == empty()) {
            return DataResult.success(Map.of(key, value));
        }
        if (input instanceof final Map<?, ?> map) {
            if (map.isEmpty()) {
                return DataResult.success(Map.of(key, value));
            }
            final Map<Object, Object> result = new HashMap<>(map.size() + 1);
            result.putAll(map);
            result.put(key, value);
            return DataResult.success(Map.copyOf(result));
        }
        return DataResult.error(() -> "Not a map: " + input);
    }

    @Override
    public DataResult<Object> mergeToMap(final Object input, final Map<Object, Object> values) {
        if (input == empty()) {
            return DataResult.success(values);
        }
        if (input instanceof final Map<?, ?> map) {
            if (map.isEmpty()) {
                return DataResult.success(values);
            }
            final Map<Object, Object> result = new HashMap<>(map.size() + values.size());
            result.putAll(map);
            result.putAll(values);
            return DataResult.success(Map.copyOf(result));
        }
        return DataResult.error(() -> "Not a map: " + input);
    }

    private static Map<Object, Object> mapLikeToMap(final MapLike<Object> values) {
        return values.entries().collect(Collectors.toUnmodifiableMap(Pair::getFirst, Pair::getSecond));
    }

    @Override
    public DataResult<Object> mergeToMap(final Object input, final MapLike<Object> values) {
        if (input == empty()) {
            return DataResult.success(mapLikeToMap(values));
        }
        if (input instanceof final Map<?, ?> map) {
            if (map.isEmpty()) {
                return DataResult.success(mapLikeToMap(values));
            }

            final Map<Object, Object> result = new HashMap<>(map.size());
            result.putAll(map);
            values.entries().forEach(e -> result.put(e.getFirst(), e.getSecond()));
            return DataResult.success(Map.copyOf(result));
        }
        return DataResult.error(() -> "Not a map: " + input);
    }

    private static Stream<Pair<Object, Object>> getMapEntries(final Map<?, ?> input) {
        return input.entrySet().stream().map(e -> Pair.of(e.getKey(), e.getValue()));
    }

    @Override
    public DataResult<Stream<Pair<Object, Object>>> getMapValues(final Object input) {
        if (input instanceof final Map<?, ?> map) {
            return DataResult.success(getMapEntries(map));
        }
        return DataResult.error(() -> "Not a map: " + input);
    }

    @Override
    public DataResult<Consumer<BiConsumer<Object, Object>>> getMapEntries(final Object input) {
        if (input instanceof final Map<?, ?> map) {
            return DataResult.success(map::forEach);
        }
        return DataResult.error(() -> "Not a map: " + input);
    }

    @Override
    public Object createMap(final Stream<Pair<Object, Object>> map) {
        return map.collect(Collectors.toUnmodifiableMap(Pair::getFirst, Pair::getSecond));
    }

    @Override
    public DataResult<MapLike<Object>> getMap(final Object input) {
        if (input instanceof final Map<?, ?> map) {
            return DataResult.success(
                    new MapLike<>() {
                        @Nullable
                        @Override
                        public Object get(final Object key) {
                            return map.get(key);
                        }

                        @Nullable
                        @Override
                        public Object get(final String key) {
                            return map.get(key);
                        }

                        @Override
                        public Stream<Pair<Object, Object>> entries() {
                            return getMapEntries(map);
                        }

                        @Override
                        public String toString() {
                            return "MapLike[" + map + "]";
                        }
                    }
            );
        }
        return DataResult.error(() -> "Not a map: " + input);
    }

    @Override
    public Object createMap(final Map<Object, Object> map) {
        return map;
    }

    @Override
    public DataResult<Stream<Object>> getStream(final Object input) {
        if (input instanceof final List<?> list) {
            return DataResult.success(list.stream().map(o -> o));
        }
        return DataResult.error(() -> "Not an list: " + input);
    }

    @Override
    public DataResult<Consumer<Consumer<Object>>> getList(final Object input) {
        if (input instanceof final List<?> list) {
            return DataResult.success(list::forEach);
        }
        return DataResult.error(() -> "Not an list: " + input);
    }

    @Override
    public Object createList(final Stream<Object> input) {
        return input.toList();
    }

    @Override
    public DataResult<ByteBuffer> getByteBuffer(final Object input) {
        if (input instanceof final ByteList value) {
            return DataResult.success(ByteBuffer.wrap(value.toByteArray()));
        }
        return DataResult.error(() -> "Not a byte list: " + input);
    }

    @Override
    public Object createByteList(final ByteBuffer input) {
        // Set .limit to .capacity to match default method
        final ByteBuffer wholeBuffer = input.duplicate().clear();
        final ByteArrayList result = new ByteArrayList();
        result.size(wholeBuffer.capacity());
        wholeBuffer.get(0, result.elements(), 0, result.size());
        return result;
    }

    @Override
    public DataResult<IntStream> getIntStream(final Object input) {
        if (input instanceof final IntList value) {
            return DataResult.success(value.intStream());
        }
        return DataResult.error(() -> "Not an int list: " + input);
    }

    @Override
    public Object createIntList(final IntStream input) {
        return IntArrayList.toList(input);
    }

    @Override
    public DataResult<LongStream> getLongStream(final Object input) {
        if (input instanceof final LongList value) {
            return DataResult.success(value.longStream());
        }
        return DataResult.error(() -> "Not a long list: " + input);
    }

    @Override
    public Object createLongList(final LongStream input) {
        return LongArrayList.toList(input);
    }

    @Override
    public Object remove(final Object input, final String key) {
        if (input instanceof final Map<?, ?> map) {
            final Map<Object, Object> result = new LinkedHashMap<>(map);
            result.remove(key);
            return Map.copyOf(result);
        }
        return input;
    }

    @Override
    public RecordBuilder<Object> mapBuilder() {
        return new FixedMapBuilder<>(this);
    }

    @Override
    public String toString() {
        return "Java";
    }

    private static final class FixedMapBuilder<T> extends RecordBuilder.AbstractUniversalBuilder<T, Map<T, T>> {
        public FixedMapBuilder(final DynamicOps<T> ops) {
            super(ops);
        }

        @Override
        protected Map<T, T> initBuilder() {
            return new HashMap<>();
        }

        @Override
        protected Map<T, T> append(final T key, final T value, final Map<T, T> builder) {
            builder.put(key, value);
            return builder;
        }

        @Override
        protected DataResult<T> build(final Map<T, T> builder, final T prefix) {
            final Map<T, T> result = Map.copyOf(builder);
            return ops().mergeToMap(prefix, result);
        }
    }
}
