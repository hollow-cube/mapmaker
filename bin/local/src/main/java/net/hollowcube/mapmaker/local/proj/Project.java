package net.hollowcube.mapmaker.local.proj;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.hollowcube.mapmaker.map.script.loader.ScriptManifest;
import net.hollowcube.mapmaker.util.gson.EnumTypeAdapter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public record Project(
        @NotNull String name,
        @NotNull List<Script> scripts
) {
    public static final String FILE_NAME = "mmproj.json";

    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(ScriptManifest.Type.class, new EnumTypeAdapter<>(ScriptManifest.Type.class))
            .disableJdkUnsafe()
            .create();

    public static @NotNull Project read(@NotNull Path projectPath) {
        try {
            final Path projectFile = projectPath.resolve(FILE_NAME);
            return GSON.fromJson(Files.readString(projectFile), Project.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public record Script(@NotNull ScriptManifest.Type type, @NotNull String path) {

    }
}
