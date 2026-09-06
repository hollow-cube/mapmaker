package net.hollowcube.mapmaker.util.gson;

import com.google.gson.*;
import net.hollowcube.ipc.map.MapBuilder;
import net.hollowcube.ipc.map.MapData;
import net.hollowcube.ipc.map.MapSlot;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.ArrayList;

public final class LegacyMapSlotDeserializer implements JsonDeserializer<MapSlot> {
    @Override
    public MapSlot deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
        var object = json.getAsJsonObject();
        var map = context.<MapData>deserialize(object.get("map"), MapData.class);
        var createdAt = context.<Instant>deserialize(object.get("createdAt"), Instant.class);
        var role = object.get("role");
        var owner = role != null && !role.isJsonNull() && "owner".equals(role.getAsString());
        var builders = new ArrayList<MapBuilder>();
        if (object.get("builders") instanceof JsonArray array) {
            for (var builder : array) builders.add(context.deserialize(builder, MapBuilder.class));
        }
        return new MapSlot(map, createdAt, owner, builders);
    }
}
