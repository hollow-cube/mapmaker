package net.hollowcube.mapmaker.util;

import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static net.minestom.server.network.NetworkBuffer.VAR_INT;

@SuppressWarnings("UnstableApiUsage")
public final class ProtocolUtil {

    public static <K, V> void writeMap(
        NetworkBuffer buffer,
        NetworkBuffer.Type<K> keyType,
        NetworkBuffer.Type<V> valueType,
        Map<K, V> map
    ) {
        buffer.write(VAR_INT, map.size());
        for (var entry : map.entrySet()) {
            buffer.write(keyType, entry.getKey());
            buffer.write(valueType, entry.getValue());
        }
    }

    public static <K, V> void writeMap(
        NetworkBuffer buffer,
        NetworkBuffer.Type<K> keyType,
        BiConsumer<V, NetworkBuffer> valueWriter,
        Map<K, V> map
    ) {
        buffer.write(VAR_INT, map.size());
        for (var entry : map.entrySet()) {
            buffer.write(keyType, entry.getKey());
            valueWriter.accept(entry.getValue(), buffer);
        }
    }

    public static <K, V> void writeMap(
        NetworkBuffer buffer,
        BiConsumer<K, NetworkBuffer> keyWriter,
        BiConsumer<V, NetworkBuffer> valueWriter,
        Map<K, V> map
    ) {
        buffer.write(VAR_INT, map.size());
        for (var entry : map.entrySet()) {
            keyWriter.accept(entry.getKey(), buffer);
            valueWriter.accept(entry.getValue(), buffer);
        }
    }

    public static <K, V> Map<K, V> readMap(
        NetworkBuffer buffer,
        NetworkBuffer.Type<K> keyType,
        Function<NetworkBuffer, V> valueReader
    ) {
        int size = buffer.read(VAR_INT);
        var map = new HashMap<K, V>(size);
        for (int i = 0; i < size; i++) {
            map.put(buffer.read(keyType), valueReader.apply(buffer));
        }
        return map;
    }

    public static <K, V> Map<K, V> readMap(
        NetworkBuffer buffer,
        NetworkBuffer.Type<K> keyType,
        NetworkBuffer.Type<V> valueType
    ) {
        int size = buffer.read(VAR_INT);
        var map = new HashMap<K, V>(size);
        for (int i = 0; i < size; i++) {
            map.put(buffer.read(keyType), buffer.read(valueType));
        }
        return map;
    }

    public static int getVarIntSize(int input) {
        return (input & 0xFFFFFF80) == 0
                ? 1 : (input & 0xFFFFC000) == 0
                ? 2 : (input & 0xFFE00000) == 0
                ? 3 : (input & 0xF0000000) == 0
                ? 4 : 5;
    }

    public static void writeVarInt(ByteBuffer buf, int value) {
        if ((value & (0xFFFFFFFF << 7)) == 0) {
            buf.put((byte) value);
        } else if ((value & (0xFFFFFFFF << 14)) == 0) {
            buf.putShort((short) ((value & 0x7F | 0x80) << 8 | (value >>> 7)));
        } else if ((value & (0xFFFFFFFF << 21)) == 0) {
            buf.put((byte) (value & 0x7F | 0x80));
            buf.put((byte) ((value >>> 7) & 0x7F | 0x80));
            buf.put((byte) (value >>> 14));
        } else if ((value & (0xFFFFFFFF << 28)) == 0) {
            buf.putInt((value & 0x7F | 0x80) << 24 | (((value >>> 7) & 0x7F | 0x80) << 16)
                    | ((value >>> 14) & 0x7F | 0x80) << 8 | (value >>> 21));
        } else {
            buf.putInt((value & 0x7F | 0x80) << 24 | ((value >>> 7) & 0x7F | 0x80) << 16
                    | ((value >>> 14) & 0x7F | 0x80) << 8 | ((value >>> 21) & 0x7F | 0x80));
            buf.put((byte) (value >>> 28));
        }
    }

    public static void writeVarIntHeader(@NotNull ByteBuffer buffer, int startIndex, int value) {
        buffer.put(startIndex, (byte) (value & 0x7F | 0x80));
        buffer.put(startIndex + 1, (byte) ((value >>> 7) & 0x7F | 0x80));
        buffer.put(startIndex + 2, (byte) (value >>> 14));
    }

    public static int readVarInt(ByteBuffer buf) {
        // https://github.com/jvm-profiling-tools/async-profiler/blob/a38a375dc62b31a8109f3af97366a307abb0fe6f/src/converter/one/jfr/JfrReader.java#L393
        int result = 0;
        for (int shift = 0; ; shift += 7) {
            byte b = buf.get();
            result |= (b & 0x7f) << shift;
            if (b >= 0) {
                return result;
            }
        }
    }

}
