package net.hollowcube.mapmaker.map;

import net.hollowcube.datafix.DataType;
import net.minestom.server.codec.Codec;

public enum SaveStateType {
    EDITING, PLAYING, VERIFYING;

    public static <T> Serializer<T> serializer(String name, Codec<T> codec, DataType dataType) {
        record SerializerImpl<T>(String name, Codec<T> codec, DataType dataType) implements Serializer<T> {
        }
        return new SerializerImpl<>(name, codec, dataType);
    }

    public interface Serializer<T> {
        String name();

        Codec<T> codec();

        DataType dataType();
    }
}
