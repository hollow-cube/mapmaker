package net.hollowcube.mapmaker.util;

import com.google.gson.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class Templates {
    private static final Map<String, JsonElement> TEMPLATES = new HashMap<>();

    public static @NotNull JsonObject applyObject(@NotNull String name, @NotNull Map<String, Object> context) {
        return apply(loadTemplate(name), context).getAsJsonObject();
    }

    private static @NotNull JsonElement apply(@NotNull JsonElement template, @NotNull Map<String, Object> context) {
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
                    if (result == null) throw new IllegalArgumentException("Missing context value: " + value);
                    if (result instanceof JsonElement json) {
                        yield json;
                    } else if (result instanceof String str) {
                        yield new JsonPrimitive(str);
                    } else if (result instanceof Boolean b) {
                        yield new JsonPrimitive(b);
                    } else if (result instanceof Number n) {
                        yield new JsonPrimitive(n);
                    } else {
                        throw new IllegalArgumentException("Invalid context value: " + value + " " + result.getClass());
                    }
                } else {
                    yield prim;
                }
            }
            default -> throw new IllegalArgumentException("Invalid template element: " + template);
        };
    }

    private static @NotNull JsonElement loadTemplate(@NotNull String name) {
        return TEMPLATES.computeIfAbsent(name, n -> {
            try (var is = Templates.class.getResourceAsStream("/templates/" + name + ".json")) {
                if (is == null) throw new IllegalArgumentException("Template not found: " + name);

                return new Gson().fromJson(new String(is.readAllBytes()), JsonObject.class);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load template: " + name, e);
            }
        });
    }
}
