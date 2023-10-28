package net.hollowcube.mapmaker.command.map;

import net.hollowcube.canvas.internal.Controller;
import net.hollowcube.command.Command;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.gui.play.ListMapsView;
import net.hollowcube.mapmaker.map.MapPlayerData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.player.PlayerService;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.network.ConnectionManager;
import org.jetbrains.annotations.NotNull;

public class MapListCommand extends Command {
    private static final ConnectionManager CONNECTION_MANAGER = MinecraftServer.getConnectionManager();

    private final Argument<String> targetArg;

    private final Controller guiController;
    private final MapService mapService;

    public MapListCommand(
            @NotNull Controller guiController,
            @NotNull PlayerService playerService,
            @NotNull MapService mapService
    ) {
        super("list");
        description = "Get info about your map slots";
        this.guiController = guiController;
        this.mapService = mapService;

        this.targetArg = CoreArgument.AnyPlayerId("target", playerService)
                .doc("The player to list maps for", "you");

        setDefaultExecutor(playerOnly(this::execute));
        addSyntax(playerOnly(this::execute), targetArg);
    }

    private void execute(@NotNull Player player, @NotNull CommandContext context) {
        MapPlayerData playerData;
        if (!context.has(targetArg)) {
            // No target specified, use self
            playerData = MapPlayerData.fromPlayer(player);
        } else {
            // Execute for the target, if they exist.
            try {
                var target = context.get(targetArg);
                var onlinePlayer = CONNECTION_MANAGER.getPlayer(target);
                if (onlinePlayer != null)
                    playerData = MapPlayerData.fromPlayer(onlinePlayer);
                else playerData = mapService.getMapPlayerData(target);
            } catch (MapService.NotFoundError ignored) {
                player.sendMessage("Player not found."); //todo
                return;
            }
        }

        guiController.show(player, c -> new ListMapsView(c, playerData));
    }

}
