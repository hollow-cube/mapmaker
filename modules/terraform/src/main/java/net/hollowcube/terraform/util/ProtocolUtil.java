package net.hollowcube.terraform.util;

import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static net.minestom.server.network.NetworkBuffer.VAR_INT;

@SuppressWarnings("UnstableApiUsage")
public final class ProtocolUtil {

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

    public static <K, V> @NotNull Map<K, V> readMap(
            @NotNull NetworkBuffer buffer,
            @NotNull Function<NetworkBuffer, K> keyReader,
            @NotNull Function<NetworkBuffer, V> valueReader
    ) {
        int size = buffer.read(VAR_INT);
        var map = new HashMap<K, V>(size);
        for (int i = 0; i < size; i++) {
            map.put(keyReader.apply(buffer), valueReader.apply(buffer));
        }
        return map;
    }

    private ProtocolUtil() {
    }
}
