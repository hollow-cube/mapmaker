package net.hollowcube.mapmaker.hub.command.map.legacy;

import net.hollowcube.mapmaker.hub.command.BaseHubCommand;
import net.hollowcube.mapmaker.map.MapService;
import org.jetbrains.annotations.NotNull;

public class MapLegacyCommand extends BaseHubCommand {

    public MapLegacyCommand(@NotNull MapService mapService) {
        super("legacy");

        addSubcommand(new MapLegacyListCommand(mapService));
        addSubcommand(new MapLegacyImportCommand(mapService));
    }

}
