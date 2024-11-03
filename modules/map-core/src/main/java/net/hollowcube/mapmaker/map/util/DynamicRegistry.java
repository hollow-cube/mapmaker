package net.hollowcube.mapmaker.map.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.hollowcube.common.util.dfu.DFU;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class DynamicRegistry {
    private static final Logger logger = LoggerFactory.getLogger(DynamicRegistry.class);

    private static final Map<String, JsonObject> REGISTRY;

    public static <T> @NotNull Map<String, T> get(@NotNull String name, @NotNull Codec<T> codec) {
        if (!REGISTRY.containsKey(name))
            throw new RuntimeException("No registry entry for " + name);

        var obj = REGISTRY.get(name);
        var map = new HashMap<String, T>();
        for (var entry : obj.entrySet()) {
            var result = DFU.unwrap(codec.decode(JsonOps.INSTANCE, entry.getValue())).getFirst();
            map.put(entry.getKey(), result);
        }
        return Map.copyOf(map);
    }

    static {
        var entries = new HashMap<String, JsonObject>();
        try (var is = DynamicRegistry.class.getResourceAsStream("/dynamic.json")) {
            if (is != null) {
                var obj = new Gson().fromJson(new String(is.readAllBytes(), StandardCharsets.UTF_8), JsonObject.class);
                for (var entry : obj.entrySet()) entries.put(entry.getKey(), entry.getValue().getAsJsonObject());
            } else {
                logger.warn("No registry present in build");
            }
        } catch (IOException e) {
            logger.error("Failed to load dynamic.json", e);
        } finally {
            REGISTRY = Map.copyOf(entries);
        }
    }
}
