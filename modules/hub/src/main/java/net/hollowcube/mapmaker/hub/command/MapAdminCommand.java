package net.hollowcube.mapmaker.hub.command;

import net.hollowcube.mapmaker.hub.Handler;
import net.hollowcube.mapmaker.hub.HubServer;
import net.hollowcube.mapmaker.model.PlayerData;
import net.hollowcube.mapmaker.permission.PlatformPermission;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutionException;

public class MapAdminCommand extends BaseHubCommand {

    private final HubServer server;
    private final Handler handler;

    public MapAdminCommand(@NotNull HubServer server, @NotNull Handler handler) {
        super("admin");
        this.server = server;
        this.handler = handler;

        addCondition((sender, command) -> {
            if (!(sender instanceof Player player)) return false;
            var playerData = PlayerData.fromPlayer(player);
            return server.platformPermissions().checkPermission(playerData.getId(), PlatformPermission.MAP_ADMIN);
        });

        setDefaultExecutor((sender, context) -> {
            sender.sendMessage("permission check test");
        });
    }
}
