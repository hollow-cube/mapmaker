package net.hollowcube.nbs;

import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

final class NBSTypes {
    public static final NetworkBuffer.Type<Byte> BYTE = NetworkBuffer.BYTE;
    public static final NetworkBuffer.Type<Short> UNSIGNED_BYTE = NetworkBuffer.BYTE
            .transform(b -> (short) (b & 0xFF), Short::byteValue);
    public static final NetworkBuffer.Type<Boolean> BOOL = NetworkBuffer.BOOLEAN;
    public static final NetworkBuffer.Type<Short> SHORT = NetworkBuffer.SHORT.transform(Short::reverseBytes, Short::reverseBytes);
    public static final NetworkBuffer.Type<Integer> INT = NetworkBuffer.INT.transform(Integer::reverseBytes, Integer::reverseBytes);

    public static final NetworkBuffer.Type<String> STRING = new NetworkBuffer.Type<>() {
        @Override
        public void write(@NotNull NetworkBuffer buffer, String value) {
            buffer.write(INT, value.length());
            buffer.write(NetworkBuffer.RAW_BYTES, value.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public String read(@NotNull NetworkBuffer buffer) {
            int length = buffer.read(INT);
            if (length > buffer.readableBytes())
                throw new IndexOutOfBoundsException("String length " + length + " exceeds readable bytes " + buffer.readableBytes());
            return new String(buffer.read(NetworkBuffer.FixedRawBytes(length)), StandardCharsets.UTF_8);
        }
    };
}
