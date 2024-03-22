package net.hollowcube.mapmaker.map.setting;

import com.google.gson.JsonElement;
import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.map.MapUpdateRequest;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;
import java.util.function.Function;

public record MapSettingImpl<T>(
        @NotNull String key, @NotNull T defaultValue,
        @NotNull Function<T, JsonElement> serialize,
        @NotNull Function<JsonElement, T> deserialize
) implements MapSetting<T> {

    @Override
    public T read(@NotNull MapSettings mapSettings) {
        var settings = mapSettings.extra();
        if (!settings.has(key)) return defaultValue;
        var value = deserialize.apply(settings.get(key));
        return value == null ? defaultValue : value;
    }

    @Override
    public void write(@NotNull MapSettings settings, @NotNull T newValue) {
        settings.modifyUpdateRequest(updateRequest -> {
            var value = serialize.apply(newValue);
            settings.extra().add(key, value);
            updateRequest.setExtraUpdate(key, value);
        });
    }

    public record Embedded(
            @NotNull String key,
            @NotNull Function<MapSettings, Boolean> read,
            @NotNull BiConsumer<MapUpdateRequest, Boolean> write
    ) implements MapSetting<Boolean> {

        @Override
        public @NotNull Boolean defaultValue() {
            return false;
        }

        @Override
        public @NotNull Boolean read(@NotNull MapSettings settings) {
            return read.apply(settings);
        }

        @Override
        public void write(@NotNull MapSettings settings, @NotNull Boolean newValue) {
            settings.modifyUpdateRequest(updates -> write.accept(updates, newValue));
        }
    }
}
