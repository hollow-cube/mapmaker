package net.hollowcube.mapmaker.command.map;

import net.hollowcube.canvas.internal.Controller;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument2;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.util.CoreArgument;
import net.hollowcube.mapmaker.gui.play.ListMapsView;
import net.hollowcube.mapmaker.map.MapPlayerData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.player.PlayerService;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MapListCommand extends CommandDsl {

    private final Argument2<String> targetArg;

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

        this.targetArg = CoreArgument.AnyPlayerId("target", playerService);
//                .doc("The player to list maps for", "you");

        addSyntax(playerOnly(this::execute));
        addSyntax(playerOnly(this::execute), targetArg);
    }

    private void execute(@NotNull Player player, @NotNull CommandContext context) {
        String targetId;
        if (!context.has(targetArg)) {
            // No target specified, use self
            targetId = MapPlayerData.fromPlayer(player).id();
        } else {
            // Execute for the target, if they exist.
            targetId = context.get(targetArg);
            if (targetId == null) {
                //todo this null check should be handled by the argument definition itself.
                // However, using deferred success we wont know if it was an error until too late to trigger an error handler.
                player.sendMessage("no such player: " + context.getRaw(targetArg));
                return;
            }
        }

        guiController.show(player, c -> new ListMapsView(c, targetId));
    }

}
