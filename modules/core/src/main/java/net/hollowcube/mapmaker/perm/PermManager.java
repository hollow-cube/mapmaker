package net.hollowcube.mapmaker.perm;

import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

@Blocking
public interface PermManager {

    boolean hasPlatformPermission(@NotNull Player player, @NotNull PlatformPerm perm);

    boolean hasMapPermission(@NotNull Player player, @NotNull String mapId, @NotNull MapPerm perm);

    default @NotNull net.hollowcube.command.CommandCondition createPlatformCondition2(@NotNull PlatformPerm perm) {
        return (sender, context) -> sender instanceof Player && hasPlatformPermission((Player) sender, perm)
                ? net.hollowcube.command.CommandCondition.ALLOW
                : net.hollowcube.command.CommandCondition.HIDE;
    }

}
