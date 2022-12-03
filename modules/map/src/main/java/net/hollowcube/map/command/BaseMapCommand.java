package net.hollowcube.map.command;

import net.hollowcube.map.world.MapWorld;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A command which is only available in a mapmaker map (does not restrict to playing or editing)
 */
public class BaseMapCommand extends Command {

    public BaseMapCommand(@NotNull String name, @Nullable String... aliases) {
        super(name, aliases);

        setCondition(this::isInMap);
    }

    private boolean isInMap(@Nullable CommandSender sender, @Nullable String unused) {
        if (!(sender instanceof Player player)) {
            return false;
        }
        var instance = player.getInstance();
        if (instance == null) return false;
        return instance.hasTag(MapWorld.MAP_ID);
    }
}
