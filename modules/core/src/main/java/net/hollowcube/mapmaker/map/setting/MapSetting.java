package net.hollowcube.mapmaker.map.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.map.MapUpdateRequest;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;
import java.util.function.Function;

public interface MapSetting<T> {
    static @NotNull MapSetting<Boolean> Embedded(
            @NotNull String key,
            @NotNull Function<MapSettings, Boolean> read,
            @NotNull BiConsumer<MapUpdateRequest, Boolean> write) {
        return new MapSettingImpl.Embedded(key, read, write);
    }

    static @NotNull MapSetting<Boolean> Bool(@NotNull String key, boolean defaultValue) {
        return new MapSettingImpl<>(key, defaultValue, JsonPrimitive::new, JsonElement::getAsBoolean);
    }

    static <T extends Enum<?>> @NotNull MapSetting<T> Enum(@NotNull String key, T defaultValue) {
        //noinspection unchecked
        var type = (Class<? extends Enum<?>>) defaultValue.getClass();
        var values = type.getEnumConstants();
        return new MapSettingImpl<>(key, defaultValue, v -> new JsonPrimitive(v.name()), e -> {
            var name = e.getAsString();
            for (var value : values) {
                if (value.name().equalsIgnoreCase(name)) {
                    //noinspection unchecked
                    return (T) value;
                }
            }
            return defaultValue;
        });
    }


    @NotNull
    String key();

    @NotNull
    T defaultValue();

    @ApiStatus.Internal
    T read(@NotNull MapSettings settings);

    @ApiStatus.Internal
    void write(@NotNull MapSettings settings, @NotNull T newValue);
}
