package net.hollowcube.mapmaker.command.map;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapPlayerData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.player.Permission;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static net.hollowcube.mapmaker.command.CoreCommandCondition.staffPerm;

public class MapDeleteCommand extends CommandDsl {
    private final Argument<@Nullable MapData> mapArg;
    private final Argument<String> reasonArg = Argument.GreedyString("reason")
        .description("The reason for deleting the map");

    private final MapService mapService;

    public MapDeleteCommand(MapService mapService) {
        super("delete");
        this.mapService = mapService;

        description = "Deletes a published map";
        examples = List.of("/map delete 123-456-789", "/map delete a12345bc-67de-8f91-ghij-2345k6l78912");

        mapArg = CoreArgument.Map("map", mapService)
            .description("The ID of the map to delete");

        setCondition(staffPerm(Permission.GENERIC_STAFF));
        addSyntax(playerOnly(this::handleDeleteMap), mapArg, reasonArg);
    }

    private void handleDeleteMap(Player player, CommandContext context) {
        var map = context.get(mapArg);
        var reason = context.get(reasonArg);

        if (map == null) {
            player.sendMessage(
                Component.translatable("command.play.map_not_found", Component.text(context.getRaw(mapArg))));
            return;
        }
        if (reason == null || reason.isEmpty()) {
            player.sendMessage("reason required to delete a map");
            return;
        }
        try {
            var playerData = MapPlayerData.fromPlayer(player);
            mapService.deleteMap(playerData.id(), map.id(), reason);
            player.sendMessage("deleted map " + map.id());
        } catch (Exception e) {
            player.sendMessage("failed to delete map");
            ExceptionReporter.reportException(e, player);
        }
    }
}
