package net.hollowcube.mapmaker.command;

import com.google.inject.Inject;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapPlayerData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.MapVariant;
import net.hollowcube.mapmaker.misc.MiscFunctionality;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.session.SessionManager;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TopTimesCommand extends CommandDsl {
    private final Argument<MapData> mapArg;

    private final MapService mapService;
    private final PlayerService playerService;
    private final SessionManager sessionManager;

    @Inject
    public TopTimesCommand(@NotNull MapService mapService, @NotNull PlayerService playerService, @NotNull SessionManager sessionManager) {
        super("toptimes", "tt");
        this.mapService = mapService;
        this.playerService = playerService;
        this.sessionManager = sessionManager;

        category = CommandCategories.MAP;
        description = "Lists the top 10 fastest completion times of a map";

        mapArg = CoreArgument.PlayableMap("map", mapService)
                .description("The map to check the top times of");

        addSyntax(playerOnly(this::showTopTimes));
        addSyntax(playerOnly(this::showTopTimes), mapArg);
    }

    private void showTopTimes(@NotNull Player player, @NotNull CommandContext context) {
        var currentMap = MiscFunctionality.getCurrentMap(sessionManager, mapService, player);

        var targetMap = context.get(mapArg);
        // If no map was specified, try to get the map from the player's current world.
        if (targetMap == null) targetMap = currentMap;
        // If they are not in a map (they are in the hub), try to get the last map they played.
        if (targetMap == null) {
            var lastPlayedMap = MapPlayerData.fromPlayer(player).lastPlayedMap();
            if (lastPlayedMap == null) {
                player.sendMessage("no last played map to check");
                return;
            }

            targetMap = mapService.getMap(PlayerDataV2.fromPlayer(player).id(), lastPlayedMap);
        }

        // At this point target map always contains a value.
        sendLeaderboardData(player, targetMap, currentMap == null);
    }

    private void sendLeaderboardData(@NotNull Player player, @NotNull MapData map, boolean withMapName) {
        if (map.settings().getVariant() != MapVariant.PARKOUR) {
            player.sendMessage("This map does not have a leaderboard.");
            return;
        }

        var playerData = PlayerDataV2.fromPlayer(player);
        var leaderboard = mapService.getPlaytimeLeaderboard(map.id(), playerData.id());

        var messages = leaderboard.toComponents(playerService, false);
        if (messages == null) {
            player.sendMessage("No times have been recorded yet.");
            return;
        }

        messages.forEach(player::sendMessage);
    }
}
