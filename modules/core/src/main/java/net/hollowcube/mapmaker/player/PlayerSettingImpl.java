package net.hollowcube.mapmaker.player;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public record PlayerSettingImpl<T>(
        @NotNull String key, T defaultValue,
        @NotNull Function<T, JsonElement> serialize,
        @NotNull Function<JsonElement, T> deserialize
) implements PlayerSetting<T> {

    @Override
    public @NotNull T read(@NotNull JsonObject settings) {
        if (!settings.has(key)) return defaultValue;
        var value = deserialize.apply(settings.get(key));
        return value == null ? defaultValue : value;
    }

    @Override
    public @NotNull JsonElement write(@NotNull T value) {
        return serialize.apply(value);
    }
}
