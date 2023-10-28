package net.hollowcube.mapmaker.command.map;

import net.hollowcube.command.Command;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.perm.PermManager;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MapInfoCommand extends Command {
    private final Argument<MapData> mapArg;

    public MapInfoCommand(@NotNull MapService mapService, @NotNull PermManager permManager) {
        super("info");
        description = "Get info about a map";

        mapArg = CoreArgument.AnyMap("map", mapService, permManager)
                .errorHandler(((sender, context) -> sender.sendMessage("TODO: error message")));

        addSyntax(playerOnly(this::showInfoAboutTarget), mapArg);
        setDefaultExecutor(playerOnly(this::handleMissingMapArg)); // Overridden in maps using MapListCommandMixin
    }

    private void handleMissingMapArg(@NotNull Player player, @NotNull CommandContext context) {
        player.sendMessage("something went wrong");
    }

    private void showInfoAboutTarget(@NotNull Player player, @NotNull CommandContext context) {
        player.sendMessage("todo: showInfoAboutTarget map=" + context.get(mapArg));
    }
}
