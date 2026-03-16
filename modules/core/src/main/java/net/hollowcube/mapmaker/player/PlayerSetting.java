package net.hollowcube.mapmaker.player;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Function;

//todo these should probably be serialized using dfu codecs
public sealed interface PlayerSetting<T> permits PlayerSettingImpl {

    static PlayerSetting<String> String(String key, String defaultValue) {
        return create(key, defaultValue, JsonPrimitive::new, JsonElement::getAsString);
    }

    static PlayerSetting<Boolean> Bool(String key, boolean defaultValue) {
        return create(key, defaultValue, JsonPrimitive::new, JsonElement::getAsBoolean);
    }

    static PlayerSetting<Integer> Int(String key, int defaultValue) {
        return create(key, defaultValue, JsonPrimitive::new, JsonElement::getAsInt);
    }

    static <T extends Enum<?>> PlayerSetting<T> Enum(String key, T defaultValue) {
        //noinspection unchecked
        var type = (Class<? extends Enum<?>>) defaultValue.getClass();
        var values = type.getEnumConstants();
        return create(key, defaultValue, v -> new JsonPrimitive(v.name()), e -> {
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

    static <T> PlayerSetting<T> create(
            String key, T defaultValue,
            Function<T, JsonElement> serialize,
            Function<JsonElement, T> deserialize) {
        return new PlayerSettingImpl<>(key, defaultValue, serialize, deserialize);
    }

    String key();

    T defaultValue();

    @ApiStatus.Internal
    T read(JsonObject settings);

    @ApiStatus.Internal
    JsonElement write(T value);
}
