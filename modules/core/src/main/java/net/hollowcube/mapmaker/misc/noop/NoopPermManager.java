package net.hollowcube.mapmaker.misc.noop;

import net.hollowcube.mapmaker.perm.MapPerm;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.perm.PlatformPermLike;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public class NoopPermManager implements PermManager {

    @Override
    public void invalidate(@NotNull PlatformPermLike perm, @NotNull String playerId) {

    }

    @Override
    public boolean hasPlatformPermission(@NotNull Player player, @NotNull PlatformPermLike perm) {
        return true;
    }

    @Override
    public boolean hasMapPermission(@NotNull Player player, @NotNull String mapId, @NotNull MapPerm perm) {
        return true;
    }

    @Override
    public @NotNull Predicate<String> createPrefetchedCondition(@NotNull PlatformPermLike perm) {
        return p -> true;
    }
}
