package net.hollowcube.aj.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import net.hollowcube.mql.MqlCompiler;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public record MqlCompilerOps<T>(
        @NotNull DynamicOps<T> delegate,
        @NotNull MqlCompiler compiler
) implements DynamicOps<T> {

    @Override public T empty() {
        return delegate.empty();
    }

    @Override public T emptyMap() {
        return delegate.emptyMap();
    }

    @Override public T emptyList() {
        return delegate.emptyList();
    }

    @Override public <U> U convertTo(DynamicOps<U> outOps, T input) {
        return delegate.convertTo(outOps, input);
    }

    @Override public DataResult<Number> getNumberValue(T input) {
        return delegate.getNumberValue(input);
    }

    @Override public Number getNumberValue(T input, Number defaultValue) {
        return delegate.getNumberValue(input, defaultValue);
    }

    @Override public T createNumeric(Number i) {
        return delegate.createNumeric(i);
    }

    @Override public T createByte(byte value) {
        return delegate.createByte(value);
    }

    @Override public T createShort(short value) {
        return delegate.createShort(value);
    }

    @Override public T createInt(int value) {
        return delegate.createInt(value);
    }

    @Override public T createLong(long value) {
        return delegate.createLong(value);
    }

    @Override public T createFloat(float value) {
        return delegate.createFloat(value);
    }

    @Override public T createDouble(double value) {
        return delegate.createDouble(value);
    }

    @Override public DataResult<Boolean> getBooleanValue(T input) {
        return delegate.getBooleanValue(input);
    }

    @Override public T createBoolean(boolean value) {
        return delegate.createBoolean(value);
    }

    @Override public DataResult<String> getStringValue(T input) {
        return delegate.getStringValue(input);
    }

    @Override public T createString(String value) {
        return delegate.createString(value);
    }

    @Override public DataResult<T> mergeToList(T list, T value) {
        return delegate.mergeToList(list, value);
    }

    @Override public DataResult<T> mergeToList(T list, List<T> values) {
        return delegate.mergeToList(list, values);
    }

    @Override public DataResult<T> mergeToMap(T map, T key, T value) {
        return delegate.mergeToMap(map, key, value);
    }

    @Override public DataResult<T> mergeToMap(T map, Map<T, T> values) {
        return delegate.mergeToMap(map, values);
    }

    @Override public DataResult<T> mergeToMap(T map, MapLike<T> values) {
        return delegate.mergeToMap(map, values);
    }

    @Override public DataResult<T> mergeToPrimitive(T prefix, T value) {
        return delegate.mergeToPrimitive(prefix, value);
    }

    @Override public DataResult<Stream<Pair<T, T>>> getMapValues(T input) {
        return delegate.getMapValues(input);
    }

    @Override public DataResult<Consumer<BiConsumer<T, T>>> getMapEntries(T input) {
        return delegate.getMapEntries(input);
    }

    @Override public T createMap(Stream<Pair<T, T>> map) {
        return delegate.createMap(map);
    }

    @Override public DataResult<MapLike<T>> getMap(T input) {
        return delegate.getMap(input);
    }

    @Override public T createMap(Map<T, T> map) {
        return delegate.createMap(map);
    }

    @Override public DataResult<Stream<T>> getStream(T input) {
        return delegate.getStream(input);
    }

    @Override public DataResult<Consumer<Consumer<T>>> getList(T input) {
        return delegate.getList(input);
    }

    @Override public T createList(Stream<T> input) {
        return delegate.createList(input);
    }

    @Override public DataResult<ByteBuffer> getByteBuffer(T input) {
        return delegate.getByteBuffer(input);
    }

    @Override public T createByteList(ByteBuffer input) {
        return delegate.createByteList(input);
    }

    @Override public DataResult<IntStream> getIntStream(T input) {
        return delegate.getIntStream(input);
    }

    @Override public T createIntList(IntStream input) {
        return delegate.createIntList(input);
    }

    @Override public DataResult<LongStream> getLongStream(T input) {
        return delegate.getLongStream(input);
    }

    @Override public T createLongList(LongStream input) {
        return delegate.createLongList(input);
    }

    @Override public T remove(T input, String key) {
        return delegate.remove(input, key);
    }

    @Override public boolean compressMaps() {
        return delegate.compressMaps();
    }

    @Override public DataResult<T> get(T input, String key) {
        return delegate.get(input, key);
    }

    @Override public DataResult<T> getGeneric(T input, T key) {
        return delegate.getGeneric(input, key);
    }

    @Override public T set(T input, String key, T value) {
        return delegate.set(input, key, value);
    }

    @Override public T update(T input, String key, Function<T, T> function) {
        return delegate.update(input, key, function);
    }

    @Override public T updateGeneric(T input, T key, Function<T, T> function) {
        return delegate.updateGeneric(input, key, function);
    }

    @Override public ListBuilder<T> listBuilder() {
        return delegate.listBuilder();
    }

    @Override public RecordBuilder<T> mapBuilder() {
        return delegate.mapBuilder();
    }

    @Override public <E> Function<E, DataResult<T>> withEncoder(Encoder<E> encoder) {
        return delegate.withEncoder(encoder);
    }

    @Override public <E> Function<T, DataResult<Pair<E, T>>> withDecoder(Decoder<E> decoder) {
        return delegate.withDecoder(decoder);
    }

    @Override public <E> Function<T, DataResult<E>> withParser(Decoder<E> decoder) {
        return delegate.withParser(decoder);
    }

    @Override public <U> U convertList(DynamicOps<U> outOps, T input) {
        return delegate.convertList(outOps, input);
    }

    @Override public <U> U convertMap(DynamicOps<U> outOps, T input) {
        return delegate.convertMap(outOps, input);
    }
}
