package net.hollowcube.compat.api.packet;

import net.minestom.server.network.NetworkBuffer;

public interface ServerboundModPacket<T extends ServerboundModPacket<T>>  {

    Type<T> getType();

    record Type<T extends ServerboundModPacket<T>>(String id, NetworkBuffer.Type<T> codec) {

        public static <T extends ServerboundModPacket<T>> Type<T> of(String namespace, String path, NetworkBuffer.Type<T> codec) {
            return new Type<>("%s:%s".formatted(namespace, path), codec);
        }
    }
}
