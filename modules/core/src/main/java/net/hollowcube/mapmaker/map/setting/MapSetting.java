package net.hollowcube.mapmaker.map.setting;

import net.hollowcube.common.util.dfu.ExtraCodecs;
import net.minestom.server.codec.Codec;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("UnstableApiUsage")
public record MapSetting<T>(
        @NotNull String key,
        @NotNull T defaultValue,
        @ApiStatus.Internal @NotNull Codec<T> codec
) {

    public static final Map<String, MapSetting<?>> ID_MAP = new ConcurrentHashMap<>();
    public static final Codec<MapSetting<?>> CODEC = Codec.STRING.transform(ID_MAP::get, MapSetting::key);

    public static @NotNull MapSetting<Boolean> Bool(@NotNull String key, boolean defaultValue) {
        return new MapSetting<>(key, defaultValue, Codec.BOOLEAN);
    }

    public static @NotNull MapSetting<Integer> Int(@NotNull String key, int defaultValue, int min, int max) {
        return new MapSetting<>(key, defaultValue, ExtraCodecs.clamppedInt(min, max));
    }

    public static @NotNull MapSetting<String> String(@NotNull String key, @NotNull String defaultValue) {
        return new MapSetting<>(key, defaultValue, Codec.STRING);
    }

    public static <T extends Enum<T>> @NotNull MapSetting<T> Enum(@NotNull String key, T defaultValue) {
        //noinspection unchecked
        return new MapSetting<>(key, defaultValue, Codec.Enum((Class<T>) defaultValue.getClass()));
    }

    public MapSetting {
        MapSetting.ID_MAP.put(key, this);
    }
}
