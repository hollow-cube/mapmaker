package net.hollowcube.mapmaker.hub.command.map;

import net.hollowcube.mapmaker.bridge.HubToMapBridge;
import net.hollowcube.mapmaker.hub.command.BaseHubCommand;
import net.hollowcube.mapmaker.hub.command.map.legacy.MapLegacyCommand;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.player.PlayerService;
import org.jetbrains.annotations.NotNull;

public class MapV2Command extends BaseHubCommand {

    public MapV2Command(@NotNull PlayerService playerService, @NotNull MapService mapService, @NotNull HubToMapBridge bridge) {
        super("map");

        addSubcommand(new MapListCommand(playerService, mapService));
        addSubcommand(new MapInfoCommand());
        addSubcommand(new MapCreateCommand(mapService));
        addSubcommand(new MapAlterCommand(mapService));
        addSubcommand(new MapDeleteCommand(mapService));
        addSubcommand(new MapSpectateCommand(bridge));
        addSubcommand(new MapEditCommand(bridge));

        addSubcommand(new MapLegacyCommand(mapService));
    }

}
