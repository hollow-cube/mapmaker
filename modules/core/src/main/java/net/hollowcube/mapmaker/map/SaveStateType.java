package net.hollowcube.mapmaker.map;

import com.mojang.serialization.Codec;
import org.jetbrains.annotations.NotNull;

public enum SaveStateType {
    EDITING, PLAYING, VERIFYING;

    public interface Serializer<T> {
        @NotNull String name();

        @NotNull Codec<T> codec();
    }
}
