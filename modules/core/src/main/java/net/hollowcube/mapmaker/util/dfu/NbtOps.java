package net.hollowcube.mapmaker.util.dfu;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import org.jetbrains.annotations.Nullable;
import org.jglrxavpok.hephaistos.nbt.*;
import org.jglrxavpok.hephaistos.nbt.mutable.MutableNBTCompound;

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

public class NbtOps implements DynamicOps<NBT> {
    public static final NbtOps INSTANCE = new NbtOps();

    private NbtOps() {
    }

    @Override
    public NBT empty() {
        return NBTEnd.INSTANCE;
    }

    @Override
    public <U> U convertTo(DynamicOps<U> outOps, NBT input) {
        return switch (input) {
            case NBTEnd nbt -> outOps.empty();
            case NBTByte nbt -> outOps.createByte(nbt.getValue());
            case NBTShort nbt -> outOps.createShort(nbt.getValue());
            case NBTInt nbt -> outOps.createInt(nbt.getValue());
            case NBTLong nbt -> outOps.createLong(nbt.getValue());
            case NBTFloat nbt -> outOps.createFloat(nbt.getValue());
            case NBTDouble nbt -> outOps.createDouble(nbt.getValue());
            case NBTByteArray nbt -> outOps.createByteList(ByteBuffer.wrap(nbt.getValue().copyArray()));
            case NBTString nbt -> outOps.createString(nbt.getValue());
            case NBTList<?> nbt -> convertList(outOps, nbt);
            case NBTCompound nbt -> convertMap(outOps, nbt);
            case NBTIntArray nbt ->
                    outOps.createIntList(IntStream.of(nbt.getValue().copyArray())); //todo seems like there should be a better way
            case NBTLongArray nbt ->
                    outOps.createLongList(LongStream.of(nbt.getValue().copyArray())); //todo seems like there should be a better way
            default -> throw new IllegalStateException("Unexpected value: " + input);
        };
    }

    @Override
    public DataResult<Number> getNumberValue(NBT input) {
        return switch (input) {
            case NBTByte nbt -> DataResult.success(nbt.getValue());
            case NBTShort nbt -> DataResult.success(nbt.getValue());
            case NBTInt nbt -> DataResult.success(nbt.getValue());
            case NBTLong nbt -> DataResult.success(nbt.getValue());
            case NBTFloat nbt -> DataResult.success(nbt.getValue());
            case NBTDouble nbt -> DataResult.success(nbt.getValue());
            default -> DataResult.error("Not a number: " + input);
        };
    }

    @Override
    public NBT createNumeric(Number number) {
        return switch (number) {
            case Byte b -> new NBTByte(b);
            case Short s -> new NBTShort(s);
            case Integer i -> new NBTInt(i);
            case Long l -> new NBTLong(l);
            case Float f -> new NBTFloat(f);
            case Double d -> new NBTDouble(d);
            default -> throw new IllegalStateException("Unexpected value: " + number);
        };
    }

    @Override
    public DataResult<Boolean> getBooleanValue(NBT input) {
        return switch (input) {
            case NBTByte nbt -> DataResult.success(nbt.getValue() != 0);
            case NBTShort nbt -> DataResult.success(nbt.getValue() != 0);
            case NBTInt nbt -> DataResult.success(nbt.getValue() != 0);
            case NBTLong nbt -> DataResult.success(nbt.getValue() != 0);
            case NBTFloat nbt -> DataResult.success(nbt.getValue() != 0);
            case NBTDouble nbt -> DataResult.success(nbt.getValue() != 0);
            default -> DataResult.error("Not a boolean: " + input);
        };
    }

    @Override
    public NBT createBoolean(boolean value) {
        return new NBTByte((byte) (value ? 1 : 0));
    }

    @Override
    public DataResult<String> getStringValue(NBT input) {
        return switch (input) {
            case NBTByte nbt -> DataResult.success(Byte.toString(nbt.getValue()));
            case NBTShort nbt -> DataResult.success(Short.toString(nbt.getValue()));
            case NBTInt nbt -> DataResult.success(Integer.toString(nbt.getValue()));
            case NBTLong nbt -> DataResult.success(Long.toString(nbt.getValue()));
            case NBTFloat nbt -> DataResult.success(Float.toString(nbt.getValue()));
            case NBTDouble nbt -> DataResult.success(Double.toString(nbt.getValue()));
            case NBTString nbt -> DataResult.success(nbt.getValue());
            default -> DataResult.error("Not a string: " + input);
        };
    }

    @Override
    public NBT createString(String value) {
        return new NBTString(value);
    }

    @Override
    public DataResult<NBT> mergeToList(NBT nbt, NBT value) {
        if (nbt instanceof NBTList<?> list) {
            if (!list.getSubtagType().equals(value.getID())) {
                return DataResult.error("Could not insert " + value.getID() + " into list of " + list.getSubtagType());
            }

            var newList = new ArrayList<NBT>(list.getValue());
            newList.add(value);
            return DataResult.success(new NBTList<>(list.getSubtagType(), newList));
        }

        if (nbt == null || nbt.getID().equals(NBTType.TAG_End)) {
            return DataResult.success(new NBTList<>(value.getID(), List.of(value)));
        }

        return DataResult.error("Could not append " + value.getID() + " to " + nbt.getID());
    }

    @Override
    public DataResult<NBT> mergeToList(NBT nbt, List<NBT> values) {
        if (nbt instanceof NBTList<?> list) {
            if (values.isEmpty()) {
                return DataResult.success(nbt);
            }
            if (!list.getSubtagType().equals(values.get(0).getID())) {
                return DataResult.error("Could not insert " + values.get(0).getID() + " into list of " + list.getSubtagType());
            }

            var newList = new ArrayList<NBT>(list.getValue());
            newList.addAll(values);
            return DataResult.success(new NBTList<>(list.getSubtagType(), newList));
        }

        if (nbt == null || nbt.getID().equals(NBTType.TAG_End)) {
            if (values.isEmpty())
                return DataResult.success(NBTEnd.INSTANCE);
            return DataResult.success(new NBTList<>(values.get(0).getID(), values));
        }

        return DataResult.error("Could not append to " + nbt.getID());
    }

    @Override
    public DataResult<NBT> mergeToMap(NBT input, NBT key, NBT value) {
        if (!(key instanceof NBTString keyString)) {
            return DataResult.error("Key is not a string: " + key);
        }
        if (input == null || input instanceof NBTEnd) {
            return DataResult.success(new NBTCompound(Map.of(keyString.getValue(), value)));
        }
        if (!(input instanceof NBTCompound compound)) {
            return DataResult.error("Not a map: " + input);
        }
        //noinspection unchecked
        return DataResult.success(compound.withEntries(Map.entry(keyString.getValue(), value)));
    }

    @Override
    public DataResult<NBT> mergeToMap(NBT input, MapLike<NBT> values) {
        if (input == null || input instanceof NBTEnd) {
            var result = new MutableNBTCompound();
            values.entries().forEach(e -> result.set(((NBTString) e.getFirst()).getValue(), e.getSecond()));
            return DataResult.success(result.toCompound());
        }
        if (!(input instanceof NBTCompound compound)) {
            return DataResult.error("Not a map: " + input);
        }
        var result = new MutableNBTCompound();
        result.putAll(compound);
        values.entries().forEach(e -> result.set(((NBTString) e.getFirst()).getValue(), e.getSecond()));
        return DataResult.success(result.toCompound());
    }

    @Override
    public DataResult<Stream<Pair<NBT, NBT>>> getMapValues(NBT input) {
        if (!(input instanceof NBTCompound compound)) {
            return DataResult.error("Not a map: " + input);
        }
        return DataResult.success(compound.getEntries().stream().map(entry -> Pair.of(createString(entry.getKey()), entry.getValue())));
    }

    @Override
    public DataResult<Consumer<BiConsumer<NBT, NBT>>> getMapEntries(NBT input) {
        if (!(input instanceof NBTCompound compound)) {
            return DataResult.error("Not a map: " + input);
        }
        return DataResult.success(c -> {
            for (var entry : compound.getEntries()) {
                c.accept(createString(entry.getKey()), entry.getValue() instanceof NBTEnd ? null : entry.getValue());
            }
        });
    }

    @Override
    public DataResult<MapLike<NBT>> getMap(NBT input) {
        if (!(input instanceof NBTCompound compound)) {
            return DataResult.error("Not a map: " + input);
        }
        return DataResult.success(new MapLike<>() {
            @Nullable
            @Override
            public NBT get(NBT keyNbt) {
                var entry = compound.get(((NBTString) keyNbt).getValue());
                return entry instanceof NBTEnd ? null : entry;
            }

            @Nullable
            @Override
            public NBT get(String key) {
                var entry = compound.get(key);
                return entry instanceof NBTEnd ? null : entry;
            }

            @Override
            public Stream<Pair<NBT, NBT>> entries() {
                return compound.getEntries().stream().map(e -> Pair.of(new NBTString(e.getKey()), e.getValue()));
            }
        });
    }

    @Override
    public NBT createMap(Stream<Pair<NBT, NBT>> map) {
        var result = new MutableNBTCompound();
        map.forEach(e -> result.set(((NBTString) e.getFirst()).getValue(), e.getSecond()));
        return result.toCompound();
    }

    @Override
    public DataResult<Stream<NBT>> getStream(NBT input) {
        if (input instanceof NBTList<?> list) {
            return DataResult.success(list.getValue().stream().map(e -> e instanceof NBTEnd ? null : e));
        }
        return DataResult.error("not a list: " + input);
    }

    @Override
    public DataResult<Consumer<Consumer<NBT>>> getList(NBT input) {
        if (input instanceof NBTList<?> list) {
            return DataResult.success(c -> {
                for (var entry : list.getValue()) {
                    c.accept(entry);
                }
            });
        }
        return DataResult.error("not a list: " + input);
    }

    @Override
    public NBT createList(Stream<NBT> input) {
        var values = input.toList();
        if (values.isEmpty()) {
            return NBTEnd.INSTANCE;
        }
        return new NBTList<>(values.get(0).getID(), values);
    }

    @Override
    public NBT remove(NBT input, String key) {
        if (input instanceof NBTCompound compound) {
            return compound.withRemovedKeys(key);
        }
        return null;
    }

    @Override
    public String toString() {
        return "NBT";
    }

    @Override
    public ListBuilder<NBT> listBuilder() {
        return DynamicOps.super.listBuilder();
    }

    private static final class ArrayBuilder implements ListBuilder<NBT> {
        private DataResult<List<NBT>> builder = DataResult.success(new ArrayList<>(), Lifecycle.stable());

        @Override
        public DynamicOps<NBT> ops() {
            return INSTANCE;
        }

        @Override
        public ListBuilder<NBT> add(NBT value) {
            builder = builder.map(b -> {
                b.add(value);
                return b;
            });
            return this;
        }

        @Override
        public ListBuilder<NBT> add(DataResult<NBT> value) {
            builder = builder.apply2stable((b, element) -> {
                b.add(element);
                return b;
            }, value);
            return this;
        }

        @Override
        public ListBuilder<NBT> withErrorsFrom(DataResult<?> result) {
            builder = builder.flatMap(r -> result.map(v -> r));
            return this;
        }

        @Override
        public ListBuilder<NBT> mapError(UnaryOperator<String> onError) {
            builder = builder.mapError(onError);
            return this;
        }

        @Override
        public DataResult<NBT> build(NBT prefix) {
            DataResult<NBT> result = builder.flatMap(b -> ops().mergeToList(prefix, b));
            builder = DataResult.success(new ArrayList<>(), Lifecycle.stable());
            return result;
        }
    }

    @Override
    public RecordBuilder<NBT> mapBuilder() {
        return new NbtRecordBuilder();
    }

    private class NbtRecordBuilder extends RecordBuilder.AbstractStringBuilder<NBT, MutableNBTCompound> {
        protected NbtRecordBuilder() {
            super(NbtOps.this);
        }

        @Override
        protected MutableNBTCompound initBuilder() {
            return new MutableNBTCompound();
        }

        @Override
        protected MutableNBTCompound append(String key, NBT value, MutableNBTCompound builder) {
            builder.set(key, value);
            return builder;
        }

        @Override
        protected DataResult<NBT> build(MutableNBTCompound builder, NBT prefix) {
            if (prefix == null || prefix instanceof NBTEnd) {
                return DataResult.success(builder.toCompound());
            }
            if (prefix instanceof NBTCompound compound) {
                var result = new MutableNBTCompound();
                result.putAll(compound);
                result.putAll(builder);
                return DataResult.success(result.toCompound());
            }
            return DataResult.error("mergeToMap called with not a map: " + prefix);
        }
    }
}
