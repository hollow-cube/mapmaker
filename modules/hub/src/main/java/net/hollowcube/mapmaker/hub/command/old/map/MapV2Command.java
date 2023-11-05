package net.hollowcube.mapmaker.hub.command.old.map;

import net.hollowcube.mapmaker.bridge.HubToMapBridge;
import net.hollowcube.mapmaker.hub.command.BaseHubCommand;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.player.PlayerService;
import org.jetbrains.annotations.NotNull;

public class MapV2Command extends BaseHubCommand {

    public MapV2Command(
            @NotNull PlayerService playerService,
            @NotNull MapService mapService,
            @NotNull HubToMapBridge bridge,
            @NotNull PermManager permManager
    ) {
        super("map");

        addSubcommand(new MapListCommand(playerService, mapService, permManager));
        addSubcommand(new MapInfoCommand());
        addSubcommand(new MapCreateCommand(mapService));
        addSubcommand(new MapAlterCommand(mapService));
        addSubcommand(new MapDeleteCommand(mapService));
        addSubcommand(new MapSpectateCommand(bridge));
        addSubcommand(new MapEditCommand(bridge));
    }

}
