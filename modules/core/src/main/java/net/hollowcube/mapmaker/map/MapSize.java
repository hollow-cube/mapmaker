package net.hollowcube.mapmaker.map;

import org.jetbrains.annotations.Nullable;

public enum MapSize {
    NORMAL(1000),
    LARGE(5000),
    MASSIVE(10000),
    COLOSSAL(50000),
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
