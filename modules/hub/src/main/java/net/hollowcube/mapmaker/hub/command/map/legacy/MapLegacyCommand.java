package net.hollowcube.mapmaker.hub.command.map.legacy;

import net.hollowcube.command.Command;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.perm.PermManager;
import org.jetbrains.annotations.NotNull;

public class MapLegacyCommand extends Command {
    public MapLegacyCommand(@NotNull MapService mapService, @NotNull PermManager permManager) {
        super("legacy");

        addSubcommand(new MapLegacyListCommand(mapService, permManager));
        addSubcommand(new MapLegacyImportCommand(mapService, permManager));
    }
}
