package net.hollowcube.mapmaker.map;

import ca.spottedleaf.dataconverter.minecraft.datatypes.MCDataType;
import com.mojang.serialization.Codec;
import org.jetbrains.annotations.NotNull;

public enum SaveStateType {
    EDITING, PLAYING, VERIFYING;

    public static <T> Serializer<T> serializer(String name, Codec<T> codec, MCDataType dataType) {
        return new Serializer<>() {
            @Override
            public @NotNull String name() {
                return name;
            }

            @Override
            public @NotNull Codec<T> codec() {
                return codec;
            }

            @Override
            public @NotNull MCDataType dataType() {
                return dataType;
            }
        };
    }

    public interface Serializer<T> {
        @NotNull String name();

        @NotNull Codec<T> codec();

        @NotNull MCDataType dataType();
    }
}
