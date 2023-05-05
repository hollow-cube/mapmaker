package net.hollowcube.mapmaker.permission;

import org.jetbrains.annotations.NotNull;

public class PlatformPermissionManagerNoop implements PlatformPermissionManager {
    @Override
    public boolean checkPermission(@NotNull String playerId, @NotNull PlatformPermission permission) {
        return true;
    }
}
