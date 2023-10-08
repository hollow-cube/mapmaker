package net.hollowcube.mapmaker.perm;

import net.minestom.server.command.builder.condition.CommandCondition;
import net.minestom.server.command.builder.exception.ArgumentSyntaxException;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

@Blocking
public interface PermManager {

    boolean hasPlatformPermission(@NotNull Player player, @NotNull PlatformPerm perm);

    boolean hasMapPermission(@NotNull Player player, @NotNull String mapId, @NotNull MapPerm perm);

    default @NotNull CommandCondition createPlatformCondition(@NotNull PlatformPerm perm) {
        return (sender, cmd) -> {
            boolean result = sender instanceof Player && hasPlatformPermission((Player) sender, perm);
            if (!result && cmd != null) {
                sender.sendMessage("no perm");
                System.out.println("CONDITION");
                throw new ArgumentSyntaxException("no perm", cmd, 0);
            }
            return result;
        };
    }

}
