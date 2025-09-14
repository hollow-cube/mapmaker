package net.hollowcube.mapmaker.runtime.freeform.bundle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.hollowcube.common.util.RuntimeGson;
import net.hollowcube.mapmaker.util.gson.EnumTypeAdapter;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Script loader for the local file system.
///
/// Useful for local development until we have a fancier way.
public class LocalFsLoader implements ScriptBundle.Loader {
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(ScriptBundle.Entrypoint.Type.class, new EnumTypeAdapter<>(ScriptBundle.Entrypoint.Type.class))
            .disableJdkUnsafe()
            .create();

    @RuntimeGson
    public record MapJson(String id, List<ScriptBundle.Entrypoint> entrypoints) {
    }

    // Mapping of map.json -> id to fs path.
    private final Map<String, Path> availableBundles = new HashMap<>();

    public LocalFsLoader(Path basePath) {
        try (var list = Files.list(basePath)) {
            for (var file : list.toList()) {
                if (!Files.isDirectory(file)) continue;

                var mapJsonFile = file.resolve("map.json");
                if (!Files.exists(mapJsonFile)) continue;

                var mapJson = GSON.fromJson(Files.readString(mapJsonFile), JsonObject.class);
                availableBundles.put(mapJson.get("id").getAsString(), file.toRealPath());
            }
        } catch (IOException e) {
            throw new RuntimeException("failed to discover scripts", e);
        }
    }

    @Override
    public @Nullable ScriptBundle load(String id) {
        var path = availableBundles.get(id);
        if (path == null) return null;

        try {
            var mapJsonFile = path.resolve("map.json");
            var mapJson = GSON.fromJson(Files.readString(mapJsonFile), MapJson.class);

            return new ScriptBundle() {
                @Override
                public String id() {
                    return mapJson.id();
                }

                @Override
                public List<Entrypoint> entrypoints() {
                    return mapJson.entrypoints();
                }

                @Override
                public Script loadScript(String name) {
                    var scriptFile = path.resolve(name);
                    if (!Files.exists(scriptFile))
                        throw new RuntimeException("script " + name + " not found in " + path);
                    try {
                        return new Script(scriptFile.getFileName().toString(), Files.readString(scriptFile));
                    } catch (IOException e) {
                        throw new RuntimeException("failed to load script " + name + ":", e);
                    }
                }
            };
        } catch (IOException e) {
            throw new RuntimeException("failed to load script " + id + ":", e);
        }
    }
}
