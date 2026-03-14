package net.hollowcube.mapmaker.util;

import com.google.gson.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class Templates {
    private static final Map<String, JsonElement> TEMPLATES = new HashMap<>();

    private Templates() {
    }

    public static JsonObject applyObject(String name, Map<String, Object> context) {
        return apply(loadTemplate(name), context).getAsJsonObject();
    }

    private static JsonElement apply(JsonElement template, Map<String, Object> context) {
        return switch (template) {
            case JsonObject obj -> {
                var result = new JsonObject();
                for (var entry : obj.entrySet()) {
                    result.add(entry.getKey(), apply(entry.getValue(), context));
                }
                yield result;
            }
            case JsonArray arr -> {
                var result = new JsonArray();
                for (var element : arr) {
                    result.add(apply(element, context));
                }
                yield result;
            }
            case JsonPrimitive prim -> {
                var value = prim.getAsString();
                if (value.startsWith("$")) {
                    var result = context.get(value.substring(1));
                    yield switch (result) {
                        case JsonElement json -> json;
                        case String str -> new JsonPrimitive(str);
                        case Boolean b -> new JsonPrimitive(b);
                        case Number n -> new JsonPrimitive(n);
                        case null -> throw new IllegalArgumentException("Missing context value: " + value);
                        default -> throw new IllegalArgumentException("Invalid context value: " + value + " " + result.getClass());
                    };
                } else {
                    yield prim;
                }
            }
            default -> throw new IllegalArgumentException("Invalid template element: " + template);
        };
    }

    private static JsonElement loadTemplate(String name) {
        return TEMPLATES.computeIfAbsent(name, _ -> {
            try (var is = Templates.class.getResourceAsStream("/templates/" + name + ".json")) {
                if (is == null) throw new IllegalArgumentException("Template not found: " + name);

                return new Gson().fromJson(new String(is.readAllBytes()), JsonObject.class);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load template: " + name, e);
            }
        });
    }
}
