package net.hollowcube.mapmaker.permission;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import net.hollowcube.common.config.SpiceDBConfig;
import net.hollowcube.mapmaker.permission.client.SpiceDBClientFactory;
import org.jetbrains.annotations.NotNull;

public interface PlatformPermissionManager {

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
    static @NotNull ListenableFuture<@NotNull PlatformPermissionManager> spicedb(@NotNull SpiceDBConfig config) {
        var clientFactory = SpiceDBClientFactory.get();
        return Futures.transform(
                clientFactory.newPermissionClient(config),
                PlatformPermissionManagerSpiceDB::new,
                Runnable::run
        );
    }

    @NotNull ListenableFuture<Boolean> checkPermission(@NotNull String playerId, @NotNull PlatformPermission permission);

}
