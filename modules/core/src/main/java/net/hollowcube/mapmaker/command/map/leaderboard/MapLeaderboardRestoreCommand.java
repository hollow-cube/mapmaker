package net.hollowcube.mapmaker.command.map.leaderboard;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.api.maps.MapClient;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.player.PlayerData;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MapLeaderboardRestoreCommand extends CommandDsl {
    private final Argument<@Nullable MapData> mapArg;

    private final MapService mapService;

    public MapLeaderboardRestoreCommand(@NotNull MapService mapService, @NotNull MapClient maps) {
        super("restore");
        this.mapService = mapService;

        description = "Syncs the leaderboard with internal source of truth. Do not use unless you know this is correct";

        mapArg = CoreArgument.Map("map", maps)
                .description("The ID of the map to restore");

        addSyntax(playerOnly(this::handleRestoreLeaderboard), mapArg);
    }

    private void handleRestoreLeaderboard(@NotNull Player player, @NotNull CommandContext context) {
        var map = context.get(mapArg);
        if (map == null) {
            player.sendMessage(
                    Component.translatable("command.play.map_not_found", Component.text(context.getRaw(mapArg))));
            return;
        }

        var playerId = PlayerData.fromPlayer(player).id();
        try {
            mapService.restorePlaytimeLeaderboard(playerId, map.id());
            player.sendMessage("restored for " + map.settings().getName());
        } catch (Exception e) {
            player.sendMessage("failed to restore leaderboard");
            ExceptionReporter.reportException(e, player);
        }
    }
}
