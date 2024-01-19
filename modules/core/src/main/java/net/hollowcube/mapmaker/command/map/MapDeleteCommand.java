package net.hollowcube.mapmaker.command.map;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapPlayerData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.perm.PlatformPerm;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MapDeleteCommand extends CommandDsl {
    private final Argument<@NotNull MapData> mapArg;
    private final Argument<String> reasonArg = Argument.GreedyString("reason");

    private final MapService mapService;

    public MapDeleteCommand(@NotNull MapService mapService, @NotNull PermManager permManager) {
        super("delete");
        this.mapService = mapService;

        mapArg = CoreArgument.PlayableMap("map", mapService);

        setCondition(permManager.createPlatformCondition2(PlatformPerm.MAP_ADMIN));
        addSyntax(playerOnly(this::handleDeleteMap), mapArg, reasonArg);
    }

    private void handleDeleteMap(@NotNull Player player, @NotNull CommandContext context) {
        var map = context.get(mapArg);
        var reason = context.get(reasonArg);
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
            MinecraftServer.getExceptionManager().handleException(e);
        }
    }
}
