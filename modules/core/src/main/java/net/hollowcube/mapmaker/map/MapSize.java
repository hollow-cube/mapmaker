package net.hollowcube.mapmaker.map;

import org.jetbrains.annotations.Nullable;

public enum MapSize {
    NORMAL(150),
    LARGE(300),
    MASSIVE(600),
    COLOSSAL(1200),
    UNLIMITED(29_999_984 /*Minecraft world limit*/) // POWER!!!
    ;

    private final int size;

    MapSize(int size) {
        this.size = size;
    }

    public int size() {
        return size;
    }

    public static @Nullable MapSize fromIntegerSize(int size) {
        for (MapSize mapSize : values()) {
            if (mapSize.size == size) {
                return mapSize;
            }
        }
        return null;
    }
}
