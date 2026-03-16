package net.hollowcube.mapmaker.map.setting;

import net.hollowcube.common.util.dfu.ExtraCodecs;
import net.minestom.server.codec.Codec;
import org.jetbrains.annotations.ApiStatus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public record MapSetting<T>(
    String key,
    T defaultValue,
    @ApiStatus.Internal Codec<T> codec
) {

    public static final Map<String, MapSetting<?>> ID_MAP = new ConcurrentHashMap<>();
    public static final Codec<MapSetting<?>> CODEC = Codec.STRING.transform(ID_MAP::get, MapSetting::key);

    public static MapSetting<Boolean> Bool(String key, boolean defaultValue) {
        return new MapSetting<>(key, defaultValue, Codec.BOOLEAN);
    }

    public static MapSetting<Integer> Int(String key, int defaultValue, int min, int max) {
        return new MapSetting<>(key, defaultValue, ExtraCodecs.clamppedInt(min, max));
    }

    public static MapSetting<String> String(String key, String defaultValue) {
        return new MapSetting<>(key, defaultValue, Codec.STRING);
    }

    public static <T extends Enum<T>> MapSetting<T> Enum(String key, T defaultValue) {
        //noinspection unchecked
        return new MapSetting<>(key, defaultValue, Codec.Enum((Class<T>) defaultValue.getClass()));
    }

    public MapSetting {
        MapSetting.ID_MAP.put(key, this);
    }
}
