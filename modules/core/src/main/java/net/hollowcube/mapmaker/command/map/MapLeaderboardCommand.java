package net.hollowcube.mapmaker.command.map;

import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.map.leaderboard.MapLeaderboardDeleteCommand;
import net.hollowcube.mapmaker.command.map.leaderboard.MapLeaderboardRestoreCommand;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.perm.PlatformPerm;
import net.hollowcube.mapmaker.player.PlayerService;
import org.jetbrains.annotations.NotNull;

public class MapLeaderboardCommand extends CommandDsl {

    public MapLeaderboardCommand(@NotNull PlayerService playerService, @NotNull MapService mapService, @NotNull PermManager permManager) {
        super("lb");

        description = "Remove or restore the times of a map";

        setCondition(permManager.createPlatformCondition2(PlatformPerm.MAP_ADMIN));

        addSubcommand(new MapLeaderboardDeleteCommand(playerService, mapService));
        addSubcommand(new MapLeaderboardRestoreCommand(mapService));
    }

}
