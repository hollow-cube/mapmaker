package net.hollowcube.mapmaker.command.map;

import net.hollowcube.canvas.internal.Controller;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.gui.play.list.MapListView;
import net.hollowcube.mapmaker.map.MapPlayerData;
import net.hollowcube.mapmaker.player.PlayerService;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MapListCommand extends CommandDsl {

    private final Argument<String> targetArg;

    private final Controller guis;

    public MapListCommand(@NotNull Controller guis, @NotNull PlayerService players) {
        super("list");
        this.guis = guis;

        description = "Show all the maps published by a player";

        this.targetArg = CoreArgument.AnyPlayerId("target", players)
                .description("The player you want to see the maps of");

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

        guis.show(player, c -> new MapListView(c, targetId));
    }

}
