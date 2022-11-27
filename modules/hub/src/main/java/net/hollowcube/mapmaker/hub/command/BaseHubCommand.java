package net.hollowcube.mapmaker.hub.command;

import net.hollowcube.mapmaker.hub.HubServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A command which is only available in a mapmaker hub world.
 */
class BaseHubCommand extends Command {
    public BaseHubCommand(@NotNull String name, @Nullable String... aliases) {
        super(name, aliases);

        setCondition(this::isInHub);
    }

    private boolean isInHub(@Nullable CommandSender sender, @Nullable String unused) {
        if (!(sender instanceof Player player)) {
            return false;
        }
        var instance = player.getInstance();
        if (instance == null) return false;
        return instance.hasTag(HubServer.HUB_MARKER);
    }
}
