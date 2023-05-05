package net.hollowcube.mapmaker.permission;

import net.hollowcube.common.config.SpiceDBConfig;
import net.hollowcube.mapmaker.permission.client.SpiceDBClientFactory;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

public @Blocking interface PlatformPermissionManager {

    /**
     * Returns a permission manager that does nothing and always reports positive permission results.
     * <p>
     * Obviously it is very unsafe for this to be used in production and should be prevented at all costs.
     */
    static @NotNull PlatformPermissionManager noop() {
        return new PlatformPermissionManagerNoop();
    }

    /**
     * Returns a permission manager that uses SpiceDB as the backend.
     * <p>
     * This is the default permission manager used by MapMaker.
     */
    @Blocking
    static @NotNull PlatformPermissionManager spicedb(@NotNull SpiceDBConfig config) {
        var client = SpiceDBClientFactory.get().newPermissionClient(config);
        return new PlatformPermissionManagerSpiceDB(client);
    }

    @Blocking boolean checkPermission(@NotNull String playerId, @NotNull PlatformPermission permission);

}
