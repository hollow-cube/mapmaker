package net.hollowcube.mapmaker.config;

import com.google.gson.*;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

record ConfigLoaderV3Impl(@NotNull JsonObject root) implements ConfigLoaderV3 {
    private static final String ENV_PREFIX = "MAPMAKER";

    private static final Logger logger = LoggerFactory.getLogger(ConfigLoaderV3Impl.class);
    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setPrettyPrinting()
            .disableJdkUnsafe()
            .create();

    public static @NotNull ConfigLoaderV3Impl loadDefault(String[] args) {
        try (var is = ConfigLoaderV3Impl.class.getResourceAsStream("/default_config.json")) {
            if (is == null) {
                logger.error("default_config.json not present in binary");
                System.exit(1);
            }

            var envmap = new HashMap<>(System.getenv());
            // Load env overrides from cli args
            for (var arg : args) {
                var split = arg.split("=");
                if (split.length != 2) continue;
                envmap.put(split[0], split[1]);
            }
            // Load env overrides from .env and vault injector
            var extraPaths = List.of(".env", "/vault/secrets/service");
            for (var pathString : extraPaths) {
                var path = Path.of(pathString);
                if (!Files.exists(path)) continue;

                for (var line : Files.readAllLines(path)) {
                    if (line.isBlank() || line.startsWith("#")) continue;
                    var split = line.split("=");
                    if (split.length != 2) continue;
                    envmap.put(split[0].trim(), split[1].trim());
                }
            }

            return loadFromText(is.readAllBytes(), envmap);
        } catch (IOException e) {
            logger.error("failed to load config file", e);

            // Probably should shutdown gracefully, but this is in theory the first thing that runs so it doesnt matter that much.
            System.exit(1);
            throw new RuntimeException(e); // Does nothing besides stop the compiler from complaining about the exit above.
        }
    }

    public static @NotNull ConfigLoaderV3Impl loadFromText(byte @NotNull [] text, @NotNull Map<String, String> env) {
        try {
            var root = GSON.fromJson(new String(text, StandardCharsets.UTF_8), JsonObject.class);
            root = (JsonObject) replaceEnvVarOverrides(env, new ArrayList<>(), root);

            //todo: load secrets from secret file.
            return new ConfigLoaderV3Impl(root);
        } catch (Exception e) {
            logger.error("failed to load config file", e);

            // Probably should shutdown gracefully, but this is in theory the first thing that runs so it doesnt matter that much.
            System.exit(1);
            throw new RuntimeException(e); // Does nothing besides stop the compiler from complaining about the exit above.
        }
    }

    @Override
    public <C> @NotNull C get(@NotNull Class<C> clazz) {
        var path = clazz.getSimpleName().replace("Config", "")
                .toLowerCase(Locale.ROOT);
        var element = GSON.fromJson(root.get(path), clazz);
        Check.notNull(element, "Config path " + path + " not found. Add entry in default_config.json.");
        return element;
    }

    @Override
    public void dump() {
        System.out.println(GSON.toJson(root));
    }

    private static JsonElement replaceEnvVarOverrides(@NotNull Map<String, String> env, List<String> path, JsonElement element) {
        return switch (element) {
            case JsonObject object -> {
                JsonObject replaced = new JsonObject();
                for (var entry : object.entrySet()) {
                    path.add(entry.getKey());
                    replaced.add(entry.getKey(), replaceEnvVarOverrides(env, path, entry.getValue()));
                    path.removeLast();
                }
                yield replaced;
            }
            case JsonArray array -> {
                JsonArray replaced = new JsonArray();
                for (int i = 0; i < array.size(); i++) {
                    path.add(Integer.toString(i));
                    replaced.add(replaceEnvVarOverrides(env, path, array.get(i)));
                    path.removeLast();
                }
                yield replaced;
            }
            case JsonPrimitive primitive -> {
                var envKey = (ENV_PREFIX + "_" + String.join("_", path)).toUpperCase(Locale.ROOT);
                var value = env.get(envKey);
                if (value == null) yield primitive;
                yield new JsonPrimitive(value);
            }
            case JsonNull ignored -> {
                var envKey = (ENV_PREFIX + "_" + String.join("_", path)).toUpperCase(Locale.ROOT);
                var value = env.get(envKey);
                if (value == null) yield JsonNull.INSTANCE;
                yield new JsonPrimitive(value);
            }
            case null, default -> element;
        };
    }
}
