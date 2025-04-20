package net.hollowcube.datafix.util;

import net.minestom.server.codec.Result;
import net.minestom.server.codec.Transcoder;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public final class TranscoderValue implements Transcoder<Value> {
    public static final Transcoder<Value> INSTANCE = new TranscoderValue();

    private TranscoderValue() {
    }

    @Override
    public @NotNull Value createNull() {
        return Value.NULL;
    }

    @Override
    public @NotNull Result<Boolean> getBoolean(@NotNull Value value) {
        var result = value.as(Boolean.class, null);
        return result != null ? new Result.Ok<>(result) : new Result.Error<>("not a boolean");
    }

    @Override
    public @NotNull Value createBoolean(boolean value) {
        return Value.wrap(value);
    }

    @Override
    public @NotNull Result<Byte> getByte(@NotNull Value value) {
        var result = value.as(Byte.class, null);
        return result != null ? new Result.Ok<>(result) : new Result.Error<>("not a byte");
    }

    @Override
    public @NotNull Value createByte(byte value) {
        return Value.wrap(value);
    }

    @Override
    public @NotNull Result<Short> getShort(@NotNull Value value) {
        var result = value.as(Short.class, null);
        return result != null ? new Result.Ok<>(result) : new Result.Error<>("not a short");
    }

    @Override
    public @NotNull Value createShort(short value) {
        return Value.wrap(value);
    }

    @Override
    public @NotNull Result<Integer> getInt(@NotNull Value value) {
        var result = value.as(Integer.class, null);
        return result != null ? new Result.Ok<>(result) : new Result.Error<>("not an int");
    }

    @Override
    public @NotNull Value createInt(int value) {
        return Value.wrap(value);
    }

    @Override
    public @NotNull Result<Long> getLong(@NotNull Value value) {
        var result = value.as(Long.class, null);
        return result != null ? new Result.Ok<>(result) : new Result.Error<>("not a long");
    }

    @Override
    public @NotNull Value createLong(long value) {
        return Value.wrap(value);
    }

    @Override
    public @NotNull Result<Float> getFloat(@NotNull Value value) {
        var result = value.as(Float.class, null);
        return result != null ? new Result.Ok<>(result) : new Result.Error<>("not a float");
    }

    @Override
    public @NotNull Value createFloat(float value) {
        return Value.wrap(value);
    }

    @Override
    public @NotNull Result<Double> getDouble(@NotNull Value value) {
        var result = value.as(Double.class, null);
        return result != null ? new Result.Ok<>(result) : new Result.Error<>("not a double");
    }

    @Override
    public @NotNull Value createDouble(double value) {
        return Value.wrap(value);
    }

    @Override
    public @NotNull Result<String> getString(@NotNull Value value) {
        var result = value.as(String.class, null);
        return result != null ? new Result.Ok<>(result) : new Result.Error<>("not a string");
    }

    @Override
    public @NotNull Value createString(@NotNull String value) {
        return Value.wrap(value);
    }

    @Override
    public @NotNull Result<List<Value>> getList(@NotNull Value value) {
        if (!value.isListLike()) return new Result.Error<>("not a list");
        var result = new ArrayList<Value>();
        for (var entry : value) {
            if (entry.isNull()) continue;
            result.add(entry);
        }
        return new Result.Ok<>(result);
    }

    @Override
    public @NotNull Value emptyList() {
        return Value.emptyList();
    }

    @Override
    public @NotNull ListBuilder<Value> createList(int expectedSize) {
        var list = Value.emptyList();
        return new ListBuilder<>() {
            @Override
            public @NotNull ListBuilder<Value> add(Value value) {
                list.put(list.size(0), value);
                return this;
            }

            @Override
            public Value build() {
                return list;
            }
        };
    }

    @Override
    public @NotNull Result<MapLike<Value>> getMap(@NotNull Value value) {
        return new Result.Ok<>(new MapLike<>() {
            Set<String> keys = null;

            @Override
            public @NotNull Collection<String> keys() {
                if (keys == null) {
                    keys = new HashSet<>();
                    value.forEachEntry((k, v) -> {
                        if (v.isNull()) return;
                        keys.add(k);
                    });
                }
                return keys;
            }

            @Override
            public boolean hasValue(@NotNull String key) {
                return value.getValue(key) != null;
            }

            @Override
            public @NotNull Result<Value> getValue(@NotNull String key) {
                var result = value.get(key);
                if (result.isNull()) return new Result.Error<>("no such key: " + key);
                return new Result.Ok<>(result);
            }
        });
    }

    @Override
    public @NotNull Value emptyMap() {
        return Value.emptyMap();
    }

    @Override
    public @NotNull MapBuilder<Value> createMap() {
        var map = Value.emptyMap();
        return new MapBuilder<>() {
            @Override
            public @NotNull MapBuilder<Value> put(@NotNull Value key, Value value) {
                map.put(Objects.requireNonNull(key.as(String.class, null)), value);
                return this;
            }

            @Override
            public @NotNull MapBuilder<Value> put(@NotNull String key, Value value) {
                map.put(key, value);
                return this;
            }

            @Override
            public Value build() {
                return map;
            }
        };
    }

    @Override
    public @NotNull Result<byte[]> getByteArray(@NotNull Value value) {
        return value instanceof ByteArrayValue(byte[] byteArray) ?
                new Result.Ok<>(byteArray) :
                new Result.Error<>("not a byte array");
    }

    @Override
    public @NotNull Value createByteArray(byte[] value) {
        return new ByteArrayValue(value);
    }

    @Override
    public @NotNull Result<int[]> getIntArray(@NotNull Value value) {
        return value instanceof IntArrayValue(int[] intArray) ?
                new Result.Ok<>(intArray) :
                new Result.Error<>("not an int array");
    }

    @Override
    public @NotNull Value createIntArray(int[] value) {
        return new IntArrayValue(value);
    }

    @Override
    public @NotNull Result<long[]> getLongArray(@NotNull Value value) {
        return value instanceof LongArrayValue(long[] longArray) ?
                new Result.Ok<>(longArray) :
                new Result.Error<>("not a long array");
    }

    @Override
    public @NotNull Value createLongArray(long[] value) {
        return new LongArrayValue(value);
    }

    @Override
    public @NotNull <O> Result<O> convertTo(@NotNull Transcoder<O> coder, @NotNull Value value) {
        return switch (value) {
            case NullValue _ -> new Result.Ok<>(coder.createNull());
            case ByteArrayValue(byte[] byteArray) -> new Result.Ok<>(coder.createByteArray(byteArray));
            case IntArrayValue(int[] intArray) -> new Result.Ok<>(coder.createIntArray(intArray));
            case LongArrayValue(long[] longArray) -> new Result.Ok<>(coder.createLongArray(longArray));
            case ListValue list -> {
                var result = coder.createList(list.size(0));
                for (var entry : list) {
                    if (entry.isNull()) continue;
                    switch (convertTo(coder, entry)) {
                        case Result.Ok<O> converted -> result.add(converted.value());
                        case Result.Error<?> error -> {
                            yield error.cast();
                        }
                    }
                }
                yield new Result.Ok<>(result.build());
            }
            case MapValue map -> {
                var result = coder.createMap();
                map.forEachEntry((k, v) -> {
                    if (v.isNull()) return;
                    switch (convertTo(coder, v)) {
                        case Result.Ok<O> converted -> result.put(k, converted.value());
                        case Result.Error<?> _ -> throw new IllegalStateException("Error converting map value");
                    }
                });
                yield new Result.Ok<>(result.build());
            }
            case PrimitiveValue primitive -> switch (primitive.value()) {
                case null -> new Result.Ok<>(coder.createNull());
                case Boolean b -> new Result.Ok<>(coder.createBoolean(b));
                case Byte b -> new Result.Ok<>(coder.createByte(b));
                case Short s -> new Result.Ok<>(coder.createShort(s));
                case Integer i -> new Result.Ok<>(coder.createInt(i));
                case Long l -> new Result.Ok<>(coder.createLong(l));
                case Float f -> new Result.Ok<>(coder.createFloat(f));
                case Double d -> new Result.Ok<>(coder.createDouble(d));
                case String s -> new Result.Ok<>(coder.createString(s));
                default -> new Result.Error<>("unsupported primitive type: " + primitive.value());
            };
            default -> new Result.Error<>("not a primitive value");
        };
    }
}
