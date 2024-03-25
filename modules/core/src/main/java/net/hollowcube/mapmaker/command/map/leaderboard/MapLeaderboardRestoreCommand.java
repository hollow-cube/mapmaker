package net.hollowcube.mapmaker.command.map.leaderboard;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MapLeaderboardRestoreCommand extends CommandDsl {
    private final Argument<@NotNull MapData> mapArg;

    private final MapService mapService;

    public MapLeaderboardRestoreCommand(@NotNull MapService mapService) {
        super("restore");
        this.mapService = mapService;
        
        description = "Syncs the leaderboard with internal source of truth. Do not use unless you know this is correct";

        mapArg = CoreArgument.PlayableMap("map", mapService)
                .description("The ID of the map to restore");

        addSyntax(playerOnly(this::handleRestoreLeaderboard), mapArg);
    }

    private void handleRestoreLeaderboard(@NotNull Player player, @NotNull CommandContext context) {
        var playerId = PlayerDataV2.fromPlayer(player).id();

        var map = context.get(mapArg);

        try {
            mapService.restorePlaytimeLeaderboard(playerId, map.id());
            player.sendMessage("restored for " + map.settings().getName());
        } catch (Exception e) {
            player.sendMessage("failed to restore leaderboard");
            MinecraftServer.getExceptionManager().handleException(e);
        }
    }
}
