package net.hollowcube.datafix.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.json.JSONOptions;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public final class DataFixUtils {
    public static final Gson GSON = new Gson();

    private static final GsonComponentSerializer GSON_SERIALIZER = GsonComponentSerializer.builder()
            .editOptions(b -> b.value(JSONOptions.EMIT_COMPACT_TEXT_COMPONENT, false))
            .build();

    @Contract("null -> null; !null -> !null")
    public static @Nullable String namespaced(@Nullable String value) {
        if (value == null) return null;
        try {
            return Key.key(value).toString();
        } catch (InvalidKeyException ignored) {
            return value;
        }
    }

    public static String dyeColorIdToName(int id) {
        return switch (id) {
            case 1 -> "orange";
            case 2 -> "magenta";
            case 3 -> "light_blue";
            case 4 -> "yellow";
            case 5 -> "lime";
            case 6 -> "pink";
            case 7 -> "gray";
            case 8 -> "light_gray";
            case 9 -> "cyan";
            case 10 -> "purple";
            case 11 -> "blue";
            case 12 -> "brown";
            case 13 -> "green";
            case 14 -> "red";
            case 15 -> "black";
            default -> "white";
        };
    }

    public static @Nullable Value ensureTextComponentString(Value value) {
        String raw = value.as(String.class, null);
        if (raw == null) return null;

        try {
            var object = GSON.fromJson(raw, JsonObject.class);
            return Value.wrap(object.toString());
        } catch (RuntimeException ignored) {
            // Not valid json, continue
        }
        try {
            var component = LegacyComponentSerializer.legacySection().deserialize(raw);
            return Value.wrap(GSON_SERIALIZER.serialize(component));
        } catch (RuntimeException ignored) {
            // Not valid legacy component, continue
        }
        return Value.wrap("{\"text\":\"" + raw.replace("\"", "\\\"") + "\"}");
    }

}
