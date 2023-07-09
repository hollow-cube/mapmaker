package net.hollowcube.mapmaker.hub.command.map;

import net.hollowcube.mapmaker.hub.HubHandler;
import net.hollowcube.mapmaker.hub.command.BaseHubCommand;
import net.hollowcube.mapmaker.map.MapService;
import org.jetbrains.annotations.NotNull;

public class MapV2Command extends BaseHubCommand {

    public MapV2Command(@NotNull MapService mapService, @NotNull HubHandler handler) {
        super("map");

        addSubcommand(new MapListCommand(mapService));
        addSubcommand(new MapInfoCommand());
        addSubcommand(new MapCreateCommand(mapService, handler));
        addSubcommand(new MapAlterCommand(mapService));
        addSubcommand(new MapDeleteCommand(mapService));
        addSubcommand(new MapSpectateCommand(handler));
        addSubcommand(new MapEditCommand(handler));
    }

}
