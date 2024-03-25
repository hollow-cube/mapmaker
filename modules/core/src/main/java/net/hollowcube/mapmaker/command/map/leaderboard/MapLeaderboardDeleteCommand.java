package net.hollowcube.mapmaker.command.map.leaderboard;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerService;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MapLeaderboardDeleteCommand extends CommandDsl {
    private final Argument<@NotNull MapData> mapArg;
    private final Argument<@Nullable String> playerArg;

    private final MapService mapService;

    public MapLeaderboardDeleteCommand(@NotNull PlayerService playerService, @NotNull MapService mapService) {
        super("delete");
        this.mapService = mapService;

        description = "Removes a player's or all completion times on a map";
        examples = List.of("/map lb delete 123-456-789", "/map lb delete 123-456-789 SethPRG");

        mapArg = CoreArgument.PlayableMap("map", mapService)
                .description("The ID of the map to delete entries from");
        playerArg = CoreArgument.AnyPlayerId("player", playerService)
                .description("The player (optional) to delete the entries of");

        addSyntax(playerOnly(this::handleDeleteLeaderboard), mapArg);
        addSyntax(playerOnly(this::handleDeleteLeaderboard), mapArg, playerArg);
    }

    private void handleDeleteLeaderboard(@NotNull Player player, @NotNull CommandContext context) {
        var playerId = PlayerDataV2.fromPlayer(player).id();

        var map = context.get(mapArg);
        var target = context.get(playerArg);
        if (target == null) {
            player.sendMessage("currently you cannot delete an entire leaderboard");
            return;
        }

        try {
            mapService.deletePlaytimeLeaderboard(playerId, map.id(), target);
            player.sendMessage("deleted for " + target);
        } catch (Exception e) {
            player.sendMessage("failed to delete leaderboard");
            MinecraftServer.getExceptionManager().handleException(e);
        }
    }
}
