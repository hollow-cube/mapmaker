package net.hollowcube.mapmaker.command.map;

import net.hollowcube.command.Command;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.mapmaker.command.util.CoreArgument;
import net.hollowcube.mapmaker.map.MapService;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MapLookupCommand extends Command {
    private final Argument<String> mapArg;

    public MapLookupCommand(@NotNull MapService mapService) {
        super("lookup");

        mapArg = CoreArgument.NewMapArg("map", mapService);

        addSyntax(playerOnly(this::handleMapLookup), mapArg);
    }

    private void handleMapLookup(@NotNull Player player, @NotNull CommandContext context) {
        var map = context.get(mapArg);
        player.sendMessage("Map: " + map);
    }
}
