package net.hollowcube.mapmaker.command;

import net.hollowcube.canvas.internal.Controller;
import net.hollowcube.command.Command;
import net.hollowcube.mapmaker.command.map.MapInfoCommand;
import net.hollowcube.mapmaker.command.map.MapListCommand;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.player.PlayerService;
import org.jetbrains.annotations.NotNull;

public class MapCommand extends Command {

    public final MapListCommand list;
    public final MapInfoCommand info;

    public MapCommand(
            @NotNull Controller guiController,
            @NotNull PlayerService playerService,
            @NotNull MapService mapService,
            @NotNull PermManager permManager
    ) {
        super("map");

        addSubcommand(this.list = new MapListCommand(guiController, playerService, mapService));
        addSubcommand(this.info = new MapInfoCommand(mapService, permManager));
    }

}
