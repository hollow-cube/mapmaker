package net.hollowcube.mapmaker.perm;

import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

@Blocking
public interface PermManager {

    void invalidate(@NotNull PlatformPermLike perm, @NotNull String playerId);

    boolean hasPlatformPermission(@NotNull Player player, @NotNull PlatformPermLike perm);

    boolean hasMapPermission(@NotNull Player player, @NotNull String mapId, @NotNull MapPerm perm);

    default @NotNull net.hollowcube.command.CommandCondition createPlatformCondition2(@NotNull PlatformPermLike perm) {
        return (sender, context) -> sender instanceof Player p && hasPlatformPermission(p, perm)
                ? net.hollowcube.command.CommandCondition.ALLOW
                : net.hollowcube.command.CommandCondition.HIDE;
    }

    /**
     * <p>Returns a predicate for the given permission where the result is always prefetched and will never block.</p>
     *
     * <p>The current implementation only refreshes on player join, though this is subject to change in the future.</p>
     *
     * @param perm The permission to test against
     * @return A predicate that returns true if the player has the permission
     */
    @NotNull Predicate<String> createPrefetchedCondition(@NotNull PlatformPermLike perm);

}
