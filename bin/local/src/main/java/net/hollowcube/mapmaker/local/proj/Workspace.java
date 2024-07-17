package net.hollowcube.mapmaker.local.proj;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public record Workspace(
        @NotNull String name,
        @NotNull String activeProject
) {
    public static final String FILE_NAME = "mmworkspace.json";

    public static @NotNull Workspace read(@NotNull Path workspacePath) {
        try {
            final Path projectFile = workspacePath.resolve(FILE_NAME);
            return Project.GSON.fromJson(Files.readString(projectFile), Workspace.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
