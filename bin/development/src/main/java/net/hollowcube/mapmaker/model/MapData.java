package net.hollowcube.mapmaker.model;

import org.jetbrains.annotations.NotNull;

public class MapData {

    public enum Type {
        BUILD_SHOWCASE
    }

    private final String id;
    private final Type type;
    private String name;

    public MapData(@NotNull String id, @NotNull Type type) {
        this.id = id;
        this.type = type;
    }

    public @NotNull String id() {
        return id;
    }

    public @NotNull Type type() {
        return type;
    }

    public @NotNull String name() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }
}
