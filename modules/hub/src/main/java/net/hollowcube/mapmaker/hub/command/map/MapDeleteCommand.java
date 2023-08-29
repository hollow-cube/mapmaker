package net.hollowcube.mapmaker.hub.command.map;

import net.hollowcube.common.lang.GenericMessages;
import net.hollowcube.mapmaker.hub.command.BaseHubCommand;
import net.hollowcube.mapmaker.hub.command.ExtraArguments;
import net.hollowcube.mapmaker.hub.util.HubMessages;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapPlayerData;
import net.hollowcube.mapmaker.map.MapService;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.hub.command.ExtraArguments.*;

public class MapDeleteCommand extends BaseHubCommand {
    private final Argument<MapData> mapArg = ExtraArguments.Map("map", MASK_ID | MASK_SLOT | MASK_PERSONAL_WORLD | MASK_PUBLISHED_ID);

    private final MapService mapService;

    public MapDeleteCommand(@NotNull MapService mapService) {
        super("delete");
        this.mapService = mapService;

        addSyntax(wrap(this::deleteMap), mapArg);

        //todo special error message for personal world
    }

    private void deleteMap(@NotNull Player player, @NotNull CommandContext context) {
        var map = context.get(mapArg);
        if (map == null) return;

        try {
            var playerData = MapPlayerData.fromPlayer(player);
            mapService.deleteMap(playerData, map.id());
            player.sendMessage(HubMessages.COMMAND_MAP_DELETE_SUCCESS);
        } catch (Exception e) {
            //todo handle known exception cases
            MinecraftServer.getExceptionManager().handleException(e);
            player.sendMessage(GenericMessages.COMMAND_UNKNOWN_ERROR);
        }
    }

}
