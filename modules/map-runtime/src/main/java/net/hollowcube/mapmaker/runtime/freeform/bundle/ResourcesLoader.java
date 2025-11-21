package net.hollowcube.mapmaker.runtime.freeform.bundle;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ResourcesLoader implements ScriptBundle.Loader {
    private static final Set<String> KNOWN_SCRIPTED_IDS = Set.of(
            "2d08b1c9-2193-4831-9318-75e905de8489",
            "3080cf33-8ff9-4d3e-a469-a457a896ab3d"
    );

    @Override
    public @Nullable ScriptBundle load(String id) {
        if (!KNOWN_SCRIPTED_IDS.contains(id))
            return null;

        var vfs = new HashMap<String, String>();
        var uri = "/net.hollowcube.scripting/%s.zip".formatted(id);
        try (var is = ResourcesLoader.class.getResourceAsStream(uri)) {
            if (is == null) return null;

            var zis = new ZipInputStream(is);
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;

                var content = new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                vfs.put(entry.getName(), content);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        var mapJson = LocalFsLoader.GSON.fromJson(Objects.requireNonNull(vfs.get("map.json"),
                "map.json not found in " + uri), LocalFsLoader.MapJson.class);

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
                var file = Objects.requireNonNull(vfs.get(name), "script %s not found in %s".formatted(name, uri));
                return new Script(name, file);
            }
        };
    }
}
