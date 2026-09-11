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

import static net.hollowcube.mapmaker.player.LocalPlayer.localPlayer;

public class MapLeaderboardRestoreCommand extends CommandDsl {
    private final Argument<@Nullable MapData> mapArg;

    private final ApiClient api;

    public MapLeaderboardRestoreCommand(@NotNull ApiClient api) {
        super("restore");
        this.api = api;

        description = "Syncs the leaderboard with internal source of truth. Do not use unless you know this is correct";

        mapArg = CoreArgument.Map("map", api.maps)
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

        var playerId = localPlayer(player).id();
        try {
            api.mapService.rebuildLeaderboard(map.id());
            player.sendMessage("restored for " + map.settings().name());
        } catch (Exception e) {
            player.sendMessage("failed to restore leaderboard");
            ExceptionReporter.reportException(e, player);
        }
    }
}
