package net.hollowcube.datafix.util;

import net.minestom.server.codec.Result;
import net.minestom.server.codec.Transcoder;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public final class TranscoderValue implements Transcoder<Value> {
    public static final Transcoder<Value> INSTANCE = new TranscoderValue();

    private TranscoderValue() {
    }

    @Override
    public Value createNull() {
        return Value.NULL;
    }

    @Override
    public Result<Boolean> getBoolean(Value value) {
        var result = value.as(Boolean.class, null);
        return result != null ? new Result.Ok<>(result) : new Result.Error<>("not a boolean");
    }

    @Override
    public Value createBoolean(boolean value) {
        return Value.wrap(value);
    }

    @Override
    public Result<Byte> getByte(Value value) {
        var result = value.as(Byte.class, null);
        return result != null ? new Result.Ok<>(result) : new Result.Error<>("not a byte");
    }

    @Override
    public Value createByte(byte value) {
        return Value.wrap(value);
    }

    @Override
    public Result<Short> getShort(Value value) {
        var result = value.as(Short.class, null);
        return result != null ? new Result.Ok<>(result) : new Result.Error<>("not a short");
    }

    @Override
    public Value createShort(short value) {
        return Value.wrap(value);
    }

    @Override
    public Result<Integer> getInt(Value value) {
        var result = value.as(Integer.class, null);
        return result != null ? new Result.Ok<>(result) : new Result.Error<>("not an int");
    }

    @Override
    public Value createInt(int value) {
        return Value.wrap(value);
    }

    @Override
    public Result<Long> getLong(Value value) {
        var result = value.as(Long.class, null);
        return result != null ? new Result.Ok<>(result) : new Result.Error<>("not a long");
    }

    @Override
    public Value createLong(long value) {
        return Value.wrap(value);
    }

    @Override
    public Result<Float> getFloat(Value value) {
        var result = value.as(Float.class, null);
        return result != null ? new Result.Ok<>(result) : new Result.Error<>("not a float");
    }

    @Override
    public Value createFloat(float value) {
        return Value.wrap(value);
    }

    @Override
    public Result<Double> getDouble(Value value) {
        var result = value.as(Double.class, null);
        return result != null ? new Result.Ok<>(result) : new Result.Error<>("not a double");
    }

    @Override
    public Value createDouble(double value) {
        return Value.wrap(value);
    }

    @Override
    public Result<String> getString(Value value) {
        var result = value.as(String.class, null);
        return result != null ? new Result.Ok<>(result) : new Result.Error<>("not a string");
    }

    @Override
    public Value createString(String value) {
        return Value.wrap(value);
    }

    @Override
    public Result<List<Value>> getList(Value value) {
        if (!value.isListLike()) return new Result.Error<>("not a list");
        var result = new ArrayList<Value>();
        for (var entry : value) {
            if (entry.isNull()) continue;
            result.add(entry);
        }
        return new Result.Ok<>(result);
    }

    @Override
    public Value emptyList() {
        return Value.emptyList();
    }

    @Override
    public ListBuilder<Value> createList(int expectedSize) {
        var list = Value.emptyList();
        return new ListBuilder<>() {
            @Override
            public ListBuilder<Value> add(Value value) {
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
    public Result<MapLike<Value>> getMap(Value value) {
        return new Result.Ok<>(new MapLike<>() {
            @Nullable Set<String> keys = null;

            @Override
            public Collection<String> keys() {
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
            public boolean hasValue(String key) {
                return value.getValue(key) != null;
            }

            @Override
            public Result<Value> getValue(String key) {
                var result = value.get(key);
                if (result.isNull()) return new Result.Error<>("no such key: " + key);
                return new Result.Ok<>(result);
            }
        });
    }

    @Override
    public Value emptyMap() {
        return Value.emptyMap();
    }

    @Override
    public MapBuilder<Value> createMap() {
        var map = Value.emptyMap();
        return new MapBuilder<>() {
            @Override
            public MapBuilder<Value> put(Value key, Value value) {
                map.put(Objects.requireNonNull(key.as(String.class, null)), value);
                return this;
            }

            @Override
            public MapBuilder<Value> put(String key, Value value) {
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
    public Result<byte[]> getByteArray(Value value) {
        return value instanceof ByteArrayValue(byte[] byteArray) ?
                new Result.Ok<>(byteArray) :
                new Result.Error<>("not a byte array");
    }

    @Override
    public Value createByteArray(byte[] value) {
        return new ByteArrayValue(value);
    }

    @Override
    public Result<int[]> getIntArray(Value value) {
        return value instanceof IntArrayValue(int[] intArray) ?
                new Result.Ok<>(intArray) :
                new Result.Error<>("not an int array");
    }

    @Override
    public Value createIntArray(int[] value) {
        return new IntArrayValue(value);
    }

    @Override
    public Result<long[]> getLongArray(Value value) {
        return value instanceof LongArrayValue(long[] longArray) ?
                new Result.Ok<>(longArray) :
                new Result.Error<>("not a long array");
    }

    @Override
    public Value createLongArray(long[] value) {
        return new LongArrayValue(value);
    }

    @Override
    public <O> Result<O> convertTo(Transcoder<O> coder, Value value) {
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
