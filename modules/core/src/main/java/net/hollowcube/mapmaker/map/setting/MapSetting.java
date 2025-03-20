package net.hollowcube.mapmaker.map.setting;

import com.mojang.serialization.Codec;
import net.hollowcube.common.util.dfu.ExtraCodecs;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public record MapSetting<T>(
        @NotNull String key,
        @NotNull T defaultValue,
        @ApiStatus.Internal @NotNull Codec<T> codec
) {

    public static final Map<String, MapSetting<?>> ID_MAP = new ConcurrentHashMap<>();
    public static final Codec<MapSetting<?>> CODEC = Codec.STRING.xmap(ID_MAP::get, MapSetting::key);

    public static @NotNull MapSetting<Boolean> Bool(@NotNull String key, boolean defaultValue) {
        return new MapSetting<>(key, defaultValue, Codec.BOOL);
    }

    public static @NotNull MapSetting<Integer> Int(@NotNull String key, int defaultValue, int min, int max) {
        return new MapSetting<>(key, defaultValue, ExtraCodecs.clamppedInt(min, max));
    }

    public static <T extends Enum<T>> @NotNull MapSetting<T> Enum(@NotNull String key, T defaultValue) {
        //noinspection unchecked
        return new MapSetting<>(key, defaultValue, ExtraCodecs.Enum((Class<T>) defaultValue.getClass()));
    }

    public MapSetting {
        MapSetting.ID_MAP.put(key, this);
    }
}
