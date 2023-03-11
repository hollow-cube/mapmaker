package net.hollowcube.mapmaker.permission;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.jetbrains.annotations.NotNull;

public class PlatformPermissionManagerNoop implements PlatformPermissionManager {
    @Override
    public @NotNull ListenableFuture<Boolean> checkPermission(@NotNull String playerId, @NotNull PlatformPermission permission) {
        return Futures.immediateFuture(true);
    }
}
