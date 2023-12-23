package net.hollowcube.mapmaker.hub.dep;

import net.hollowcube.mapmaker.perm.MapPerm;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.perm.PlatformPerm;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class NoopPermManager implements PermManager {
    @Override
    public boolean hasPlatformPermission(@NotNull Player player, @NotNull PlatformPerm perm) {
        return false;
    }

    @Override
    public boolean hasMapPermission(@NotNull Player player, @NotNull String mapId, @NotNull MapPerm perm) {
        return false;
    }
}
