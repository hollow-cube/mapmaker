package net.hollowcube.mapmaker.map;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public enum MapSize {
    NORMAL(0, 150, "house_1"),
    LARGE(1, 300, "house_2"),
    MASSIVE(2, 600, "house_3"),
    COLOSSAL(3, 1200, "castle"),
    UNLIMITED(-1, /*Minecraft world limit*/29_999_984, null);

    private static final List<MapSize> DISABLED_SIZES = List.of(COLOSSAL, UNLIMITED);
    public static final List<MapSize> GUI_SIZES = List.of(NORMAL, LARGE, MASSIVE, COLOSSAL);

    private final int id;
    private final int size;
    private final @Nullable String icon;

    MapSize(int id, int size, @Nullable String icon) {
        this.id = id;
        this.size = size;
        this.icon = icon;
    }

    public int id() {
        return id;
    }

    public int size() {
        return size;
    }

    public String icon() {
        return Objects.requireNonNull(icon);
    }

    public boolean unlocks(MapSize other) {
        if (DISABLED_SIZES.contains(other)) return false;
        return this.ordinal() >= other.ordinal();
    }
}
