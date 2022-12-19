package net.hollowcube.mapmaker.oldtoremove;

import org.jetbrains.annotations.NotNull;

/**
 * A description of a running version of a map and some settings.
 */
public interface MapHandle {
    int FLAG_NONE = 0;
    int FLAG_EDIT = 1;

    @NotNull String id(); // id of the map instance
    @NotNull String mapId(); // id of the map data
    int flags(); // options for the running map
}
