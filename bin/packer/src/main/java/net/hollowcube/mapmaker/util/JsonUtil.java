package net.hollowcube.mapmaker.util;

import com.google.gson.*;
import de.marhali.json5.*;
import org.jetbrains.annotations.NotNull;

public class JsonUtil {

    public static JsonElement toGson(Json5Element element) {
        return switch (element) {
            case Json5Object obj -> {
                JsonObject ret = new JsonObject();
                for (var entry : obj.entrySet()) {
                    ret.add(entry.getKey(), toGson(entry.getValue()));
                }
                yield ret;
            }
            case Json5Array arr -> {
                JsonArray ret = new JsonArray();
                for (var value : arr) {
                    ret.add(toGson(value));
                }
                yield ret;
            }
            case Json5String str -> new JsonPrimitive(str.getAsString());
            case Json5Number num -> new JsonPrimitive(num.getAsNumber());
            case Json5Boolean bool -> new JsonPrimitive(bool.getAsBoolean());
            case Json5Null ignored -> JsonNull.INSTANCE;
            default -> throw new IllegalStateException("Unexpected value: " + element);
        };
    }

    public static @NotNull JsonElement stripMinecraftNamespace(@NotNull JsonElement element) {
        if (element instanceof JsonArray array) {
            return array.asList()
                    .stream()
                    .map(JsonUtil::stripMinecraftNamespace)
                    .collect(JsonArray::new, JsonArray::add, JsonArray::addAll);
        } else if (element instanceof JsonObject object) {
            JsonObject newObject = new JsonObject();
            for (String key : object.keySet()) {
                JsonElement value = object.get(key);
                if (key.startsWith("minecraft:")) {
                    newObject.add(key.substring(10), stripMinecraftNamespace(value));
                } else {
                    newObject.add(key, stripMinecraftNamespace(value));
                }
            }
            return newObject;
        } else if (element.isJsonPrimitive()) {
            String value = element.getAsString();
            if (value.startsWith("minecraft:")) {
                return new JsonPrimitive(value.substring(10));
            } else {
                return element;
            }
        } else {
            return element;
        }
    }
}
