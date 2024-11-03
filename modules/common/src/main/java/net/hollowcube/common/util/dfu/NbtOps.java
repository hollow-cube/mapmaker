package net.hollowcube.common.util.dfu;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import net.kyori.adventure.nbt.*;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class NbtOps implements DynamicOps<BinaryTag> {
    public static final NbtOps INSTANCE = new NbtOps();

    private NbtOps() {
    }

    @Override
    public BinaryTag empty() {
        return EndBinaryTag.endBinaryTag();
    }

    @Override
    public <U> U convertTo(DynamicOps<U> outOps, BinaryTag input) {
        return switch (input) {
            case EndBinaryTag nbt -> outOps.empty();
            case ByteBinaryTag nbt -> outOps.createByte(nbt.value());
            case ShortBinaryTag nbt -> outOps.createShort(nbt.value());
            case IntBinaryTag nbt -> outOps.createInt(nbt.value());
            case LongBinaryTag nbt -> outOps.createLong(nbt.value());
            case FloatBinaryTag nbt -> outOps.createFloat(nbt.value());
            case DoubleBinaryTag nbt -> outOps.createDouble(nbt.value());
            case ByteArrayBinaryTag nbt -> outOps.createByteList(ByteBuffer.wrap(nbt.value()));
            case StringBinaryTag nbt -> outOps.createString(nbt.value());
            case ListBinaryTag nbt -> convertList(outOps, nbt);
            case CompoundBinaryTag nbt -> convertMap(outOps, nbt);
            case IntArrayBinaryTag nbt -> outOps.createIntList(IntStream.of(nbt.value()));
            case LongArrayBinaryTag nbt -> outOps.createLongList(LongStream.of(nbt.value()));
            default -> throw new IllegalStateException("Unexpected value: " + input);
        };
    }

    @Override
    public DataResult<Number> getNumberValue(BinaryTag input) {
        return switch (input) {
            case EndBinaryTag nbt -> DataResult.success(0);
            case ByteBinaryTag nbt -> DataResult.success(nbt.value());
            case ShortBinaryTag nbt -> DataResult.success(nbt.value());
            case IntBinaryTag nbt -> DataResult.success(nbt.value());
            case LongBinaryTag nbt -> DataResult.success(nbt.value());
            case FloatBinaryTag nbt -> DataResult.success(nbt.value());
            case DoubleBinaryTag nbt -> DataResult.success(nbt.value());
            default -> DataResult.error("Not a number: " + input);
        };
    }

    @Override
    public BinaryTag createNumeric(Number number) {
        return switch (number) {
            case Byte b -> ByteBinaryTag.byteBinaryTag(b);
            case Short s -> ShortBinaryTag.shortBinaryTag(s);
            case Integer i -> IntBinaryTag.intBinaryTag(i);
            case Long l -> LongBinaryTag.longBinaryTag(l);
            case Float f -> FloatBinaryTag.floatBinaryTag(f);
            case Double d -> DoubleBinaryTag.doubleBinaryTag(d);
            default -> throw new IllegalStateException("Unexpected value: " + number);
        };
    }

    @Override
    public DataResult<Boolean> getBooleanValue(BinaryTag input) {
        return switch (input) {
            case EndBinaryTag nbt -> DataResult.success(false);
            case ByteBinaryTag nbt -> DataResult.success(nbt.value() != 0);
            case ShortBinaryTag nbt -> DataResult.success(nbt.value() != 0);
            case IntBinaryTag nbt -> DataResult.success(nbt.value() != 0);
            case LongBinaryTag nbt -> DataResult.success(nbt.value() != 0);
            case FloatBinaryTag nbt -> DataResult.success(nbt.value() != 0);
            case DoubleBinaryTag nbt -> DataResult.success(nbt.value() != 0);
            default -> DataResult.error("Not a boolean: " + input);
        };
    }

    @Override
    public BinaryTag createBoolean(boolean value) {
        return ByteBinaryTag.byteBinaryTag((byte) (value ? 1 : 0));
    }

    @Override
    public DataResult<String> getStringValue(BinaryTag input) {
        return switch (input) {
            case ByteBinaryTag nbt -> DataResult.success(Byte.toString(nbt.value()));
            case ShortBinaryTag nbt -> DataResult.success(Short.toString(nbt.value()));
            case IntBinaryTag nbt -> DataResult.success(Integer.toString(nbt.value()));
            case LongBinaryTag nbt -> DataResult.success(Long.toString(nbt.value()));
            case FloatBinaryTag nbt -> DataResult.success(Float.toString(nbt.value()));
            case DoubleBinaryTag nbt -> DataResult.success(Double.toString(nbt.value()));
            case StringBinaryTag nbt -> DataResult.success(nbt.value());
            default -> DataResult.error("Not a string: " + input);
        };
    }

    @Override
    public BinaryTag createString(String value) {
        return StringBinaryTag.stringBinaryTag(value);
    }

    @Override
    public DataResult<BinaryTag> mergeToList(BinaryTag nbt, BinaryTag value) {
        if (nbt instanceof ListBinaryTag list) {
            if (!list.elementType().equals(value.type())) {
                return DataResult.error("Could not insert " + value.type() + " into list of " + list.elementType());
            }

            return DataResult.success(list.add(value));
        }

        if (nbt == null || nbt instanceof EndBinaryTag) {
            return DataResult.success(ListBinaryTag.listBinaryTag(value.type(), List.of(value)));
        }

        return DataResult.error("Could not append " + value.type() + " to " + nbt.type());
    }

    @Override
    public DataResult<BinaryTag> mergeToList(BinaryTag nbt, List<BinaryTag> values) {
        if (nbt instanceof ListBinaryTag list) {
            if (values.isEmpty()) {
                return DataResult.success(nbt);
            }
            if (!list.elementType().equals(values.get(0).type())) {
                return DataResult.error("Could not insert " + values.get(0).type() + " into list of " + list.elementType());
            }

            return DataResult.success(list.add(values));
        }

        if (nbt == null || nbt instanceof EndBinaryTag) {
            if (values.isEmpty())
                return DataResult.success(EndBinaryTag.endBinaryTag());
            return DataResult.success(ListBinaryTag.listBinaryTag(values.get(0).type(), values));
        }

        return DataResult.error("Could not append to " + nbt.type());
    }

    @Override
    public DataResult<BinaryTag> mergeToMap(BinaryTag input, BinaryTag key, BinaryTag value) {
        if (!(key instanceof StringBinaryTag keyString)) {
            return DataResult.error("Key is not a string: " + key);
        }
        if (value instanceof EndBinaryTag) {
            return DataResult.success(input);
        }
        if (input == null || input instanceof EndBinaryTag) {
            return DataResult.success(CompoundBinaryTag.from(Map.of(keyString.value(), value)));
        }
        if (!(input instanceof CompoundBinaryTag compound)) {
            return DataResult.error("Not a map: " + input);
        }
        //noinspection unchecked
        return DataResult.success(compound.put(keyString.value(), value));
    }

    @Override
    public DataResult<BinaryTag> mergeToMap(BinaryTag input, MapLike<BinaryTag> values) {
        if (input == null || input instanceof EndBinaryTag) {
            var result = CompoundBinaryTag.builder();
            values.entries().forEach(e -> {
                if (e.getSecond() instanceof EndBinaryTag) return;
                result.put(((StringBinaryTag) e.getFirst()).value(), e.getSecond());
            });
            return DataResult.success(result.build());
        }
        if (!(input instanceof CompoundBinaryTag compound)) {
            return DataResult.error("Not a map: " + input);
        }
        var result = CompoundBinaryTag.builder();
        result.put(compound);
        values.entries().forEach(e -> {
            if (e.getSecond() instanceof EndBinaryTag) return;
            result.put(((StringBinaryTag) e.getFirst()).value(), e.getSecond());
        });
        return DataResult.success(result.build());
    }

    @Override
    public DataResult<Stream<Pair<BinaryTag, BinaryTag>>> getMapValues(BinaryTag input) {
        if (!(input instanceof CompoundBinaryTag compound)) {
            return DataResult.error("Not a map: " + input);
        }
        return DataResult.success(StreamSupport.stream(compound.spliterator(), false).map(entry ->
                Pair.of(createString(entry.getKey()), entry.getValue() instanceof EndBinaryTag ? null : entry.getValue())));
    }

    @Override
    public DataResult<Consumer<BiConsumer<BinaryTag, BinaryTag>>> getMapEntries(BinaryTag input) {
        if (!(input instanceof CompoundBinaryTag compound)) {
            return DataResult.error("Not a map: " + input);
        }
        return DataResult.success(c -> {
            for (var entry : compound) {
                c.accept(createString(entry.getKey()), entry.getValue() instanceof EndBinaryTag ? null : entry.getValue());
            }
        });
    }

    @Override
    public DataResult<MapLike<BinaryTag>> getMap(BinaryTag input) {
        if (!(input instanceof CompoundBinaryTag compound)) {
            return DataResult.error("Not a map: " + input);
        }
        return DataResult.success(new MapLike<>() {
            @Nullable
            @Override
            public BinaryTag get(BinaryTag keyNbt) {
                var entry = compound.get(((StringBinaryTag) keyNbt).value());
                return entry instanceof EndBinaryTag ? null : entry;
            }

            @Nullable
            @Override
            public BinaryTag get(String key) {
                var entry = compound.get(key);
                return entry instanceof EndBinaryTag ? null : entry;
            }

            @Override
            public Stream<Pair<BinaryTag, BinaryTag>> entries() {
                return StreamSupport.stream(compound.spliterator(), false)
                        .map(e -> Pair.of(StringBinaryTag.stringBinaryTag(e.getKey()), e.getValue()));
            }
        });
    }

    @Override
    public BinaryTag createMap(Stream<Pair<BinaryTag, BinaryTag>> map) {
        var result = CompoundBinaryTag.builder();
        map.forEach(e -> {
            if (e.getSecond() instanceof EndBinaryTag) return;
            result.put(((StringBinaryTag) e.getFirst()).value(), e.getSecond());
        });
        return result.build();
    }

    @Override
    public DataResult<Stream<BinaryTag>> getStream(BinaryTag input) {
        if (input instanceof ListBinaryTag list) {
            return DataResult.success(list.stream().map(e -> e instanceof EndBinaryTag ? null : e));
        }
        return DataResult.error("not a list: " + input);
    }

    @Override
    public DataResult<Consumer<Consumer<BinaryTag>>> getList(BinaryTag input) {
        if (input instanceof ListBinaryTag list) {
            return DataResult.success(c -> {
                for (var entry : list) {
                    c.accept(entry);
                }
            });
        }
        return DataResult.error("not a list: " + input);
    }

    @Override
    public BinaryTag createList(Stream<BinaryTag> input) {
        var values = input.toList();
        if (values.isEmpty()) {
            return EndBinaryTag.endBinaryTag();
        }
        return ListBinaryTag.listBinaryTag(values.get(0).type(), values);
    }

    @Override
    public BinaryTag remove(BinaryTag input, String key) {
        if (input instanceof CompoundBinaryTag compound) {
            return compound.remove(key);
        }
        return null;
    }

    @Override
    public String toString() {
        return "NBT";
    }

    @Override
    public ListBuilder<BinaryTag> listBuilder() {
        return new ArrayBuilder();
    }

    private static final class ArrayBuilder implements ListBuilder<BinaryTag> {
        private DataResult<List<BinaryTag>> builder = DataResult.success(new ArrayList<>(), Lifecycle.stable());

        @Override
        public DynamicOps<BinaryTag> ops() {
            return INSTANCE;
        }

        @Override
        public ListBuilder<BinaryTag> add(BinaryTag value) {
            builder = builder.map(b -> {
                b.add(value);
                return b;
            });
            return this;
        }

        @Override
        public ListBuilder<BinaryTag> add(DataResult<BinaryTag> value) {
            builder = builder.apply2stable((b, element) -> {
                b.add(element);
                return b;
            }, value);
            return this;
        }

        @Override
        public ListBuilder<BinaryTag> withErrorsFrom(DataResult<?> result) {
            builder = builder.flatMap(r -> result.map(v -> r));
            return this;
        }

        @Override
        public ListBuilder<BinaryTag> mapError(UnaryOperator<String> onError) {
            builder = builder.mapError(onError);
            return this;
        }

        @Override
        public DataResult<BinaryTag> build(BinaryTag prefix) {
            DataResult<BinaryTag> result = builder.flatMap(b -> ops().mergeToList(prefix, b));
            builder = DataResult.success(new ArrayList<>(), Lifecycle.stable());
            return result;
        }
    }

    @Override
    public RecordBuilder<BinaryTag> mapBuilder() {
        return new NbtRecordBuilder();
    }

    private class NbtRecordBuilder extends RecordBuilder.AbstractStringBuilder<BinaryTag, CompoundBinaryTag.Builder> {
        protected NbtRecordBuilder() {
            super(NbtOps.this);
        }

        @Override
        protected CompoundBinaryTag.Builder initBuilder() {
            return CompoundBinaryTag.builder();
        }

        @Override
        protected CompoundBinaryTag.Builder append(String key, BinaryTag value, CompoundBinaryTag.Builder builder) {
            if (value instanceof EndBinaryTag) return builder;
            builder.put(key, value);
            return builder;
        }

        @Override
        protected DataResult<BinaryTag> build(CompoundBinaryTag.Builder builder, BinaryTag prefix) {
            if (prefix == null || prefix instanceof EndBinaryTag) {
                return DataResult.success(builder.build());
            }
            if (prefix instanceof CompoundBinaryTag compound) {
                var result = CompoundBinaryTag.builder();
                result.put(compound);
                result.put(builder.build());
                return DataResult.success(result.build());
            }
            return DataResult.error("mergeToMap called with not a map: " + prefix);
        }
    }
}
