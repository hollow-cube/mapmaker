package net.hollowcube.mapmaker.command.map.legacy;

import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.perm.PermManager;
import org.jetbrains.annotations.NotNull;

public class MapLegacyCommand extends CommandDsl {

    public MapLegacyCommand(@NotNull MapService mapService, @NotNull PermManager permManager) {
        super("legacy");

        description = "Import maps from Omega Parkour or Tapple";

        addSubcommand(new MapLegacyListCommand(mapService, permManager));
        addSubcommand(new MapLegacyImportCommand(mapService, permManager));
    }

}
