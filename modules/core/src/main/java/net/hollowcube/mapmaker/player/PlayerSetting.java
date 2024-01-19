package net.hollowcube.mapmaker.player;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public sealed interface PlayerSetting<T> permits PlayerSettingImpl {

    static @NotNull PlayerSetting<String> String(@NotNull String key, @NotNull String defaultValue) {
        return create(key, defaultValue, JsonPrimitive::new, JsonElement::getAsString);
    }

    static @NotNull PlayerSetting<Boolean> Bool(@NotNull String key, boolean defaultValue) {
        return create(key, defaultValue, JsonPrimitive::new, JsonElement::getAsBoolean);
    }

    static @NotNull PlayerSetting<Integer> Int(@NotNull String key, int defaultValue) {
        return create(key, defaultValue, JsonPrimitive::new, JsonElement::getAsInt);
    }

    static <T extends Enum<?>> @NotNull PlayerSetting<T> Enum(@NotNull String key, T defaultValue) {
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

    static <T> @NotNull PlayerSetting<T> create(
            @NotNull String key, @NotNull T defaultValue,
            @NotNull Function<T, JsonElement> serialize,
            @NotNull Function<JsonElement, T> deserialize) {
        return new PlayerSettingImpl<>(key, defaultValue, serialize, deserialize);
    }

    @NotNull String key();

    @NotNull T defaultValue();

    @ApiStatus.Internal
    @NotNull T read(@NotNull JsonObject settings);

    @ApiStatus.Internal
    @NotNull JsonElement write(@NotNull T value);
}
