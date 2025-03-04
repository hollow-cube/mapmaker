package net.hollowcube.compat.api.packet;

import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public interface ServerboundModPacket<T extends ServerboundModPacket<T>>  {

    Type<T> getType();

    record Type<T extends ServerboundModPacket<T>>(String id, NetworkBuffer.Type<T> codec) {

        public static <T extends ServerboundModPacket<T>> Type<T> of(String namespace, String path, NetworkBuffer.Type<T> codec) {
            return new Type<>("%s:%s".formatted(namespace, path), codec);
        }

        public static <T extends ServerboundModPacket<T>> ServerboundModPacket.Type<T> of(String namespace, String path, Function<NetworkBuffer, T> reader) {
            var type = new NetworkBuffer.Type<T>() {
                @Override
                public void write(@NotNull NetworkBuffer buffer, T value) {
                    throw new UnsupportedOperationException("You cannot read a read-only packet type");
                }

                @Override
                public T read(@NotNull NetworkBuffer buffer) {
                    return reader.apply(buffer);
                }
            };
            return new ServerboundModPacket.Type<>("%s:%s".formatted(namespace, path), type);
        }
    }
}
