package net.hollowcube.mapmaker.util.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.hollowcube.ipc.map.MapSize;
import net.hollowcube.ipc.player.DisplayName;
import net.hollowcube.ipc.player.PlayerData;

import java.lang.reflect.Type;
import java.time.Instant;

/// Go's player responses predate [PlayerData]: the display name is `displayNameV2` on the
/// session endpoints and `displayName` on v4, the map size is a `tempMaxMapSize` id, and
/// permissions are an unsigned decimal string.
public final class LegacyPlayerDataDeserializer implements JsonDeserializer<PlayerData> {
    @Override
    public PlayerData deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
        var obj = json.getAsJsonObject();
        var displayName = obj.has("displayName") ? obj.get("displayName") : obj.get("displayNameV2");
        var settings = obj.get("settings");
        var permissions = obj.get("permissions");
        return new PlayerData(
            obj.get("id").getAsString(),
            obj.get("username").getAsString(),
            context.deserialize(displayName, DisplayName.class),
            settings == null || settings.isJsonNull() ? new JsonObject() : settings.getAsJsonObject(),
            longOrZero(obj, "playtime"),
            longOrZero(obj, "cubits"),
            context.deserialize(obj.get("hypercubeUntil"), Instant.class),
            permissions == null ? 0 : Long.parseUnsignedLong(permissions.getAsString()),
            (int) longOrZero(obj, "mapSlots"),
            MapSize.fromId(longOrZero(obj, "tempMaxMapSize")),
            (int) longOrZero(obj, "mapBuilders"),
            longOrZero(obj, "coins"));
    }

    private static long longOrZero(JsonObject obj, String key) {
        var value = obj.get(key);
        return value == null || value.isJsonNull() ? 0 : value.getAsLong();
    }
}
