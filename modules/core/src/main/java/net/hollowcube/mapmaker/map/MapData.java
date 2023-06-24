package net.hollowcube.mapmaker.map;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;

public class MapData {
    public static final String DEFAULT_NAME = "Untitled Map";

    private String id;
    private String owner;

    private MapSettings settings;

    private long publishedId;
    private Instant publishedAt;

    public @NotNull String id() {
        return id;
    }

    public @NotNull String owner() {
        return owner;
    }

    public @NotNull MapSettings settings() {
        return settings;
    }

    public boolean isPublished() {
        return publishedId != 0;
    }
}
