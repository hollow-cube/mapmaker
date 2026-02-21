package net.hollowcube.mapmaker.command.map;

import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.map.leaderboard.MapLeaderboardDeleteCommand;
import net.hollowcube.mapmaker.command.map.leaderboard.MapLeaderboardRestoreCommand;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.player.Permission;
import net.hollowcube.mapmaker.player.PlayerService;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.command.CoreCommandCondition.staffPerm;

public class MapLeaderboardCommand extends CommandDsl {

    public MapLeaderboardCommand(@NotNull PlayerService playerService, @NotNull MapService mapService) {
        super("lb");

        description = "Remove or restore the times of a map";

        setCondition(staffPerm(Permission.GENERIC_STAFF));

        addSubcommand(new MapLeaderboardDeleteCommand(playerService, mapService));
        addSubcommand(new MapLeaderboardRestoreCommand(mapService));
    }

}
