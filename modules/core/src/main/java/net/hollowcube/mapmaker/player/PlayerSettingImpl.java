package net.hollowcube.mapmaker.player;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public record PlayerSettingImpl<T>(
    String key, T defaultValue,
    Function<T, JsonElement> serialize,
    Function<JsonElement, @Nullable T> deserialize
) implements PlayerSetting<T> {

    @Override
    public T read(JsonObject settings) {
        if (!settings.has(key)) return defaultValue;
        var value = deserialize.apply(settings.get(key));
        return value == null ? defaultValue : value;
    }

    @Override
    public JsonElement write(T value) {
        return serialize.apply(value);
    }
}
