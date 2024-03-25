package net.hollowcube.mapmaker.command.map;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.perm.PermManager;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MapInfoCommand extends CommandDsl {
//    private final Argument2<MapData> mapArg;

    public MapInfoCommand(@NotNull MapService mapService, @NotNull PermManager permManager) {
        super("info");

        description = "Shows information about a map";
        examples = List.of("/map info 123-456-789");

//        mapArg = CoreArgument.AnyMap("map", mapService, permManager);
//                .errorHandler(((sender, context) -> sender.sendMessage("TODO: error message")));

//        addSyntax(playerOnly(this::showInfoAboutTarget), mapArg);
        addSyntax(playerOnly(this::handleMissingMapArg)); // Overridden in maps using MapListCommandMixin
    }

    private void handleMissingMapArg(@NotNull Player player, @NotNull CommandContext context) {
        player.sendMessage("something went wrong");
    }

    private void showInfoAboutTarget(@NotNull Player player, @NotNull CommandContext context) {
//        player.sendMessage("todo: showInfoAboutTarget map=" + context.get(mapArg));
        player.sendMessage("TODO");
    }
}
