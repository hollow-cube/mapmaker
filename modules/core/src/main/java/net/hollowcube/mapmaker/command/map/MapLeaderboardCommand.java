package net.hollowcube.mapmaker.command.map;

import net.hollowcube.command.Command;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.mapmaker.command.util.CoreArgument;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.perm.PlatformPerm;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerService;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MapLeaderboardCommand extends Command {
    private final Argument<@NotNull MapData> mapArg;
    private final Argument<@Nullable String> playerArg;

    private final MapService mapService;

    public MapLeaderboardCommand(@NotNull PlayerService playerService, @NotNull MapService mapService, @NotNull PermManager permManager) {
        super("lb");
        this.mapService = mapService;

        mapArg = CoreArgument.PlayableMap("map", mapService);
        playerArg = Argument.Opt(CoreArgument.AnyPlayerId("player", playerService));

        setCondition(permManager.createPlatformCondition2(PlatformPerm.MAP_ADMIN));
        addSyntax(playerOnly(this::handleDeleteLeaderboard), Argument.Literal("delete"), mapArg, playerArg);
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
