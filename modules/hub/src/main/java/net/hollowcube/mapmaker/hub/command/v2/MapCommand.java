package net.hollowcube.mapmaker.hub.command.v2;

import net.hollowcube.command.Command;
import net.hollowcube.mapmaker.bridge.HubToMapBridge;
import net.hollowcube.mapmaker.hub.command.v2.map.MapListCommand;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.player.PlayerService;
import org.jetbrains.annotations.NotNull;

public class MapCommand extends Command {

    public MapCommand(
            @NotNull PlayerService playerService,
            @NotNull MapService mapService,
            @NotNull HubToMapBridge bridge,
            @NotNull PermManager permManager
    ) {
        super("map");

        addSubcommand(new MapListCommand(playerService, mapService, permManager));
    }

}
