package net.hollowcube.mapmaker.permission;

import org.jetbrains.annotations.NotNull;

/**
 * Matches the platform permissions as defined in `zed/mapmaker.zed`.
 */
public enum PlatformPermission {
    MAP_ADMIN("map_admin");

    private final String key;

    PlatformPermission(@NotNull String key) {
        this.key = key;
    }

    public @NotNull String key() {
        return key;
    }
}
