package net.hollowcube.mapmaker.map.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import net.hollowcube.common.util.dfu.ExtraCodecs;
import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.map.MapUpdateRequest;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

public interface MapSetting<T> {

    Map<String, MapSetting<?>> ID_MAP = new ConcurrentHashMap<>();
    Codec<MapSetting<?>> CODEC = Codec.STRING.xmap(ID_MAP::get, MapSetting::key);

    static @NotNull MapSetting<Boolean> Embedded(
            @NotNull String key,
            @NotNull Function<MapSettings, Boolean> read,
            @NotNull BiConsumer<MapUpdateRequest, Boolean> write) {
        return new MapSettingImpl.Embedded(key, read, write);
    }

    static @NotNull MapSetting<Boolean> Bool(@NotNull String key, boolean defaultValue) {
        return new MapSettingImpl<>(key, defaultValue, JsonPrimitive::new, JsonElement::getAsBoolean, Codec.BOOL);
    }

    static @NotNull MapSetting<Integer> Int(@NotNull String key, int defaultValue) {
        return new MapSettingImpl<>(key, defaultValue, JsonPrimitive::new, JsonElement::getAsInt, Codec.INT);
    }

    static <T extends Enum<T>> @NotNull MapSetting<T> Enum(@NotNull String key, T defaultValue) {
        //noinspection unchecked
        var type = (Class<T>) defaultValue.getClass();
        var values = type.getEnumConstants();
        return new MapSettingImpl<>(key, defaultValue, v -> new JsonPrimitive(v.name()), e -> {
            var name = e.getAsString();
            for (var value : values) {
                if (value.name().equalsIgnoreCase(name)) {
                    return value;
                }
            }
            return defaultValue;
        }, ExtraCodecs.Enum(type));
    }


    @NotNull
    String key();

    @NotNull
    T defaultValue();

    @ApiStatus.Internal
    T read(@NotNull MapSettings settings);

    @ApiStatus.Internal
    void write(@NotNull MapSettings settings, @NotNull T newValue);

    @ApiStatus.Internal
    Codec<T> codec();
}
