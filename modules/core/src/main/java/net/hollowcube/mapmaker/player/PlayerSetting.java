package net.hollowcube.mapmaker.player;

import com.mojang.serialization.Codec;
import net.hollowcube.common.util.dfu.ExtraCodecs;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public sealed interface PlayerSetting<T> {

    static @NotNull PlayerSetting<String> String(@NotNull String key, @NotNull String fallback) {
        return create(key, fallback, Codec.STRING);
    }

    static @NotNull PlayerSetting<Boolean> Bool(@NotNull String key, boolean fallback) {
        return create(key, fallback, Codec.BOOL);
    }

    static @NotNull PlayerSetting<Integer> Int(@NotNull String key, int fallback) {
        return create(key, fallback, Codec.INT);
    }

    static <T extends Enum<T>> @NotNull PlayerSetting<T> Enum(@NotNull String key, T fallback) {
        return create(key, fallback, ExtraCodecs.Enum(fallback.getDeclaringClass()));
    }

    static <T> @NotNull PlayerSetting<T> create(@NotNull String key, @NotNull T fallback, @NotNull Codec<T> codec) {
        return new Impl<>(key, fallback, codec.orElse(fallback));
    }

    @NotNull String key();

    @NotNull T fallback();

    @NotNull Codec<T> codec();

    @ApiStatus.Internal
    record Impl<T>(
            @NotNull String key,
            @NotNull T fallback,
            @NotNull Codec<T> codec
    ) implements PlayerSetting<T> {}
}
