package net.hollowcube.mapmaker.command.map.leaderboard;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.player.PlayerService;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MapLeaderboardDeleteCommand extends CommandDsl {
    private final Argument<@Nullable MapData> mapArg;
    private final Argument<@Nullable String> playerArg;
    private final Argument<?> notifyArg;

    private final PlayerService playerService;
    private final MapService mapService;

    public MapLeaderboardDeleteCommand(@NotNull PlayerService playerService, @NotNull MapService mapService) {
        super("delete");
        this.playerService = playerService;
        this.mapService = mapService;

        description = "Removes a player's or all completion times on a map";
        examples = List.of("/map lb delete 123-456-789", "/map lb delete 123-456-789 SethPRG");

        mapArg = CoreArgument.Map("map", mapService)
                .description("The ID of the map to delete entries from");
        playerArg = CoreArgument.AnyPlayerId("player", playerService)
                .description("The player (optional) to delete the entries of");
        notifyArg = Argument.Literal("notify")
                .description("Whether to notify the player(s) about the deletion");

        addSyntax(playerOnly(this::handleDeleteLeaderboard), mapArg);
        addSyntax(playerOnly(this::handleDeleteLeaderboard), mapArg, playerArg);
        addSyntax(playerOnly(this::handleDeleteLeaderboard), mapArg, playerArg, notifyArg);
    }

    private void handleDeleteLeaderboard(@NotNull Player player, @NotNull CommandContext context) {
        var map = context.get(mapArg);
        var target = context.get(playerArg);
        var notify = context.has(notifyArg);

        if (map == null) {
            player.sendMessage(
                    Component.translatable("command.play.map_not_found", Component.text(context.getRaw(mapArg))));
            return;
        }
        if (target == null) {
            player.sendMessage("currently you cannot delete an entire leaderboard");
            return;
        }

        var playerId = PlayerData.fromPlayer(player).id();
        try {
            mapService.deletePlaytimeLeaderboard(playerId, map.id(), target);
            player.sendMessage("deleted for " + target);

            if (notify) {
                playerService.createNotification(
                    target,
                    "map_time_deleted",
                    map.id(),
                    null,
                    null,
                    true
                );
            }

        } catch (Exception e) {
            player.sendMessage("failed to delete leaderboard");
            ExceptionReporter.reportException(e, playerId);
        }
    }
}
