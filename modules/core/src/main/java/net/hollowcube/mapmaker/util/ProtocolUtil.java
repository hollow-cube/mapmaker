package net.hollowcube.mapmaker.util;

import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static net.minestom.server.network.NetworkBuffer.VAR_INT;

@SuppressWarnings("UnstableApiUsage")
public final class ProtocolUtil {

    public static <K, V> void writeMap(
            @NotNull NetworkBuffer buffer,
            @NotNull NetworkBuffer.Type<K> keyType,
            @NotNull NetworkBuffer.Type<V> valueType,
            @NotNull Map<K, V> map

    ) {
        buffer.write(VAR_INT, map.size());
        for (var entry : map.entrySet()) {
            buffer.write(keyType, entry.getKey());
            buffer.write(valueType, entry.getValue());
        }
    }

    public static <K, V> void writeMap(
            @NotNull NetworkBuffer buffer,
            @NotNull NetworkBuffer.Type<K> keyType,
            @NotNull BiConsumer<V, NetworkBuffer> valueWriter,
            @NotNull Map<K, V> map

    ) {
        buffer.write(VAR_INT, map.size());
        for (var entry : map.entrySet()) {
            buffer.write(keyType, entry.getKey());
            valueWriter.accept(entry.getValue(), buffer);
        }
    }

    public static <K, V> void writeMap(
            @NotNull NetworkBuffer buffer,
            @NotNull BiConsumer<K, NetworkBuffer> keyWriter,
            @NotNull BiConsumer<V, NetworkBuffer> valueWriter,
            @NotNull Map<K, V> map

    ) {
        buffer.write(VAR_INT, map.size());
        for (var entry : map.entrySet()) {
            keyWriter.accept(entry.getKey(), buffer);
            valueWriter.accept(entry.getValue(), buffer);
        }
    }

    public static <K, V> @NotNull Map<K, V> readMap(
            @NotNull NetworkBuffer buffer,
            @NotNull NetworkBuffer.Type<K> keyType,
            @NotNull Function<NetworkBuffer, V> valueReader
    ) {
        int size = buffer.read(VAR_INT);
        var map = new HashMap<K, V>(size);
        for (int i = 0; i < size; i++) {
            map.put(buffer.read(keyType), valueReader.apply(buffer));
        }
        return map;
    }

    public static <K, V> @NotNull Map<K, V> readMap(
            @NotNull NetworkBuffer buffer,
            @NotNull NetworkBuffer.Type<K> keyType,
            @NotNull NetworkBuffer.Type<V> valueType
    ) {
        int size = buffer.read(VAR_INT);
        var map = new HashMap<K, V>(size);
        for (int i = 0; i < size; i++) {
            map.put(buffer.read(keyType), buffer.read(valueType));
        }
        return map;
    }

}
