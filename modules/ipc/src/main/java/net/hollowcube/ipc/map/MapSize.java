package net.hollowcube.ipc.map;

import java.util.List;

public enum MapSize {
    NORMAL(0, 150),
    LARGE(1, 300),
    MASSIVE(2, 600),
    COLOSSAL(3, 1200),
    UNLIMITED(4, 29_999_984),
    UNKNOWN(-2, 150),
    ;

    public static final List<MapSize> GUI_SIZES = List.of(NORMAL, LARGE, MASSIVE, COLOSSAL);
    private final int id;
    private final int size;

    MapSize(int id, int size) {
        this.id = id;
        this.size = size;
    }

    /// The size a stored id means, which is Go's `model.MapSize`: `maps.size` and
    /// `player_data.max_map_size` both hold it. Anything unrecognised reads as the smallest so
    /// that the map still opens, which includes -1 (Go's "unlimited, old, not real") and the tall
    /// sizes 5 and 6 that were never given out.
    public static MapSize fromId(long id) {
        for (var size : values()) if (size != UNKNOWN && size.id == id) return size;
        return NORMAL;
    }

    public int id() {
        return id;
    }

    public int size() {
        return size;
    }

    public boolean unlocks(MapSize other) {
        return this != UNKNOWN
            && other != UNKNOWN
            && other != COLOSSAL
            && other != UNLIMITED
            && ordinal() >= other.ordinal();
    }
}
