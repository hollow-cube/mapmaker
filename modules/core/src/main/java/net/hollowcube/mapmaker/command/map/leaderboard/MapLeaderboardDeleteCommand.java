package net.hollowcube.mapmaker.command.map.leaderboard;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.ipc.map.MapData;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

import static net.hollowcube.mapmaker.player.LocalPlayer.localPlayer;

public class MapLeaderboardDeleteCommand extends CommandDsl {
    private final Argument<@Nullable MapData> mapArg;
    private final Argument<@Nullable String> playerArg;
    private final Argument<?> notifyArg;

    private final ApiClient api;

    public MapLeaderboardDeleteCommand(@NotNull ApiClient api) {
        super("delete");
        this.api = api;

        description = "Removes a player's or all completion times on a map";
        examples = List.of("/map lb delete 123-456-789", "/map lb delete 123-456-789 SethPRG");

        mapArg = CoreArgument.Map("map", api.maps)
                .description("The ID of the map to delete entries from");
        playerArg = CoreArgument.AnyPlayerId("player", api.players)
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

        var playerId = localPlayer(player).id();
        try {
            api.mapService.deleteLeaderboardEntry(map.id(), UUID.fromString(target), notify);
            player.sendMessage("deleted for " + target);
        } catch (Exception e) {
            player.sendMessage("failed to delete leaderboard");
            ExceptionReporter.reportException(e, playerId.toString());
        }
    }
}
