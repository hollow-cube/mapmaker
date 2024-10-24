package net.hollowcube.terraform.util;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static net.minestom.server.network.NetworkBuffer.INT;
import static net.minestom.server.network.NetworkBuffer.VAR_INT;

@SuppressWarnings("UnstableApiUsage")
public final class ProtocolUtil {
    private static final int DEBUG_MARKER = 0xbaadf00d;
    private static final boolean DEBUG_MARKERS_ENABLED = Boolean.getBoolean("terraform.debug.markers");

    // Used by Axiom as an end of list marker
    public static final long MIN_POSITION_LONG = 0b1000000000000000000000000010000000000000000000000000100000000000L;


    /**
     * Inserts a 4-byte marker into the buffer. This is used to assert that the buffer is in the correct
     * position when reading to avoid having to debug where things went wrong. Only included when the
     * `terraform.debug.markers` system property is set to `true.
     *
     * <p>IT IS NOT VALID TO READ A BUFFER WHICH WAS WRITTEN WITH DEBUG MARKERS ENABLED FROM A PROCESS
     * WITH DEBUG MARKERS DISABLED. IT WILL RESULT IN READ FAILURES. DEBUG MARKERS SHOULD ONLY BE ENABLED
     * IN A DEVELOPMENT ENVIRONMENT.</p>
     */
    public static void insertMarker(@NotNull NetworkBuffer buffer) {
        if (!DEBUG_MARKERS_ENABLED) return;
        buffer.write(INT, DEBUG_MARKER);
    }

    /**
     * @see #insertMarker(NetworkBuffer)
     */
    public static void assertMarker(@NotNull NetworkBuffer buffer, @NotNull String id) {
        if (!DEBUG_MARKERS_ENABLED) return;
        int marker = buffer.read(INT);
        Check.stateCondition(marker != DEBUG_MARKER, "Buffer marker mismatch: {}", id);
    }

    public static @NotNull Pos readPos(@NotNull NetworkBuffer buffer) {
        return new Pos(buffer.read(NetworkBuffer.VECTOR3D), buffer.read(NetworkBuffer.FLOAT), buffer.read(NetworkBuffer.FLOAT));
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

    public static byte[] makeArray(int initialCapacity, @NotNull Consumer<@NotNull NetworkBuffer> writing) {
//        NetworkBuffer writer = new NetworkBuffer(initialCapacity);
//        writing.accept(writer);
//        byte[] bytes = new byte[writer.writeIndex()];
//        writer.copyTo(0, bytes, 0, bytes.length);
//        return bytes;
        // TODO(1.21.2)
        return null;
    }

    private ProtocolUtil() {
    }
}
