package net.hollowcube.mapmaker.util.gson;

import com.google.gson.*;
import net.hollowcube.ipc.Wire;
import net.hollowcube.ipc.map.*;

import java.lang.reflect.Type;
import java.util.Locale;
import java.util.UUID;

/// Go map responses use numeric published IDs and lowercase enums.
public final class LegacyMapDataDeserializer implements JsonDeserializer<MapData> {
    @Override
    public MapData deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
        if (json.isJsonNull()) return null;
        var source = json.getAsJsonObject();
        var defaults = Wire.gson()
            .toJsonTree(
                MapData.draft(
                    UUID.fromString(source.get("id").getAsString()),
                    UUID.fromString(source.get("owner").getAsString())
                )
            )
            .getAsJsonObject();
        var settings = defaults.getAsJsonObject("settings");
        source.entrySet().forEach(e -> {
            if (!e.getKey().equals("settings") && !e.getValue().isJsonNull())
                defaults.add(e.getKey(), e.getValue().deepCopy());
        });
        if (source.has("settings") && source.get("settings").isJsonObject()) {
            source.getAsJsonObject("settings")
                .entrySet()
                .forEach(e -> {
                    if (!e.getValue().isJsonNull())
                        settings.add(e.getKey(), e.getValue().deepCopy());
                });
        }
        if ("unknown".equalsIgnoreCase(defaults.get("difficulty").getAsString()))
            defaults.addProperty("difficulty", "UNRATED");
        normalizeEnum(defaults, "verification", MapVerification.class);
        normalizeEnum(defaults, "quality", MapQuality.class);
        normalizeEnum(defaults, "difficulty", MapDifficulty.class);
        normalizeEnum(settings, "size", MapSize.class);
        normalizeEnum(settings, "variant", MapVariant.class);
        normalizeEnum(settings.getAsJsonObject("leaderboard"), "format", MapLeaderboard.Format.class);
        var id = defaults.get("publishedId");
        if (id != null && !id.isJsonNull()) {
            var numeric = Long.parseLong(id.getAsString().replace("-", ""));
            if (numeric == 0) defaults.remove("publishedId");
            else defaults.addProperty("publishedId", MapData.formatPublishedId(numeric));
        }
        return Wire.gson().fromJson(defaults, MapData.class);
    }

    private static <T extends Enum<T>> void normalizeEnum(JsonObject object, String name, Class<T> type) {
        var value = object.get(name);
        if (value == null || value.isJsonNull()) return;
        var normalized = value.getAsString().toUpperCase(Locale.ROOT);
        try {
            Enum.valueOf(type, normalized);
        } catch (IllegalArgumentException ignored) {
            normalized = "UNKNOWN";
        }
        object.addProperty(name, normalized);
    }
}
