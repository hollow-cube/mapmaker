package net.hollowcube.mapmaker.permission;

import net.hollowcube.common.config.SpiceDBConfig;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.permission.client.SpiceDBClientFactory;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

public @Blocking interface MapPermissionManager {

    /**
     * Returns a permission manager that does nothing and always reports positive permission results.
     * <p>
     * Obviously it is very unsafe for this to be used in production and should be prevented at all costs.
     */
    static @NotNull MapPermissionManager noop() {
        return new MapPermissionManagerNoop();
    }

    /**
     * Returns a permission manager that uses SpiceDB as the backend.
     * <p>
     * This is the default permission manager used by MapMaker.
     */
    @Blocking
    static @NotNull MapPermissionManagerSpiceDB spicedb(@NotNull SpiceDBConfig config) {
        var client = SpiceDBClientFactory.get().newPermissionClient(config);
        return new MapPermissionManagerSpiceDB(client);
    }

    /**
     * Adds the given player as an owner of a particular map.
     *
     * @return A future containing an updated offset token
     */
    @Blocking @NotNull String addMapOwner(@NotNull String mapId, @NotNull String playerId);

    /**
     * Makes the given map public. This means that anybody has
     * {@link net.hollowcube.mapmaker.model.MapData.Permission#READ}
     * permission on the map (eg can play the map, but of course not edit it).
     *
     * @return A future containing an updated offset token for the map.
     */
    @Blocking @NotNull String makeMapPublic(@NotNull String mapId);

    /**
     * Deletes the given map from the permission manager.
     *
     * @param mapId The map id to delete
     * @return A future that contains the resulting zed token if successful
     */
    @Blocking @NotNull String deleteMap(@NotNull String mapId);

    /** Checks if the given player has the given permission on the given map. */
    @Blocking boolean checkPermission(@NotNull String mapId, @NotNull String playerId,
                                      @NotNull MapData.Permission permission);

}
