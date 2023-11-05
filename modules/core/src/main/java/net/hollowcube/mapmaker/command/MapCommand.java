package net.hollowcube.mapmaker.command;

import net.hollowcube.canvas.internal.Controller;
import net.hollowcube.command.Command;
import net.hollowcube.mapmaker.command.map.MapInfoCommand;
import net.hollowcube.mapmaker.command.map.MapLeaderboardCommand;
import net.hollowcube.mapmaker.command.map.MapListCommand;
import net.hollowcube.mapmaker.command.map.MapLookupCommand;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.player.PlayerService;
import org.jetbrains.annotations.NotNull;

public class MapCommand extends Command {

    public final MapListCommand list;
    public final MapInfoCommand info;

    public final MapLeaderboardCommand leaderboard;

    public MapCommand(
            @NotNull Controller guiController,
            @NotNull PlayerService playerService,
            @NotNull MapService mapService,
            @NotNull PermManager permManager
    ) {
        super("map");

        // Default commands
        addSubcommand(this.list = new MapListCommand(guiController, playerService, mapService));
        addSubcommand(this.info = new MapInfoCommand(mapService, permManager));

        // Permissioned commands
        addSubcommand(this.leaderboard = new MapLeaderboardCommand(playerService, mapService, permManager));

        // Testing
        addSubcommand(new MapLookupCommand(mapService));
    }

}
