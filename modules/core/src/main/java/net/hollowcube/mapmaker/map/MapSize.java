package net.hollowcube.mapmaker.map;

import org.jetbrains.annotations.NotNull;

public enum MapSize {
    NORMAL(0, 150),
    LARGE(1, 300),
    MASSIVE(2, 600),
    COLOSSAL(3, 1200),
    UNLIMITED(-1, /*Minecraft world limit*/29_999_984) // POWER!!!
    ;

    private final int id;
    private final int size;

    MapSize(int id, int size) {
        this.id = id;
        this.size = size;
    }

    public int id() {
        return id;
    }

    public int size() {
        return size;
    }

    public boolean unlocks(@NotNull MapSize other) {
        return this.ordinal() >= other.ordinal();
    }
}
