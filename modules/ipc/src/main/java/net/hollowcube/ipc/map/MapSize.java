package net.hollowcube.ipc.map;

import java.util.List;

public enum MapSize {
    NORMAL(0, 150),
    LARGE(1, 300),
    MASSIVE(2, 600),
    COLOSSAL(3, 1200),
    UNLIMITED(-1, 29_999_984),
    UNKNOWN(-2, 150),
    ;

    public static final List<MapSize> GUI_SIZES = List.of(NORMAL, LARGE, MASSIVE, COLOSSAL);
    private final int id;
    private final int size;

    MapSize(int id, int size) {
        this.id = id;
        this.size = size;
    }

    /// The size a stored id means; anything unrecognised, which nothing should have written, is
    /// read as the smallest so that the map still opens.
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
