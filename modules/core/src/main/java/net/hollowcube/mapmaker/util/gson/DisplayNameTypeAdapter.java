package net.hollowcube.mapmaker.util.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import net.hollowcube.ipc.player.DisplayName;

import java.lang.reflect.Type;
import java.util.ArrayList;

/// Go sends a display name as a bare array of parts with string types.
public final class DisplayNameTypeAdapter implements JsonDeserializer<DisplayName> {
    @Override
    public DisplayName deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
        var parts = new ArrayList<DisplayName.Part>();
        for (var element : json.getAsJsonArray()) {
            var part = element.getAsJsonObject();
            var kind = part.get("type").getAsString();
            var color = part.get("color");
            parts.add(switch (kind) {
                case "username" -> new DisplayName.Part.Username(part.get("text").getAsString(),
                    color == null || color.isJsonNull() ? null : color.getAsString());
                case "badge" -> new DisplayName.Part.Badge(part.get("text").getAsString());
                default -> new DisplayName.Part.Unknown(kind);
            });
        }
        return new DisplayName(parts);
    }
}
