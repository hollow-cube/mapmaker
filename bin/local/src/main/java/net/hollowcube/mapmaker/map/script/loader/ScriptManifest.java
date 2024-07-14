package net.hollowcube.mapmaker.map.script.loader;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public record ScriptManifest(@NotNull List<Script> entries) {

    public enum Type {
        WORLD, PLAYER
    }

    public record Script(@NotNull String id, @NotNull String filename, @NotNull Type type) {

    }

}
