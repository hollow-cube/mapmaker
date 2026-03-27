package net.hollowcube.mapmaker;

import net.hollowcube.command.CommandManager;
import net.hollowcube.command.util.HelpCommand;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.command.TopTimesCommand;
import net.hollowcube.mapmaker.command.playerinfo.PlayerInfoCommand;
import net.hollowcube.mapmaker.map.command.*;
import net.hollowcube.mapmaker.map.runtime.AbstractMapServer;
import net.hollowcube.mapmaker.runtime.parkour.command.ShowHeightCommand;
import net.hollowcube.mapmaker.runtime.parkour.command.SpectateCommand;
import org.jetbrains.annotations.NotNull;

public final class MapCommands {
    public static void registerPlayingCommands(@NotNull AbstractMapServer server, @NotNull CommandManager commandManager) {
        commandManager.register(new HelpCommand(
            "help", new String[]{"h"},
            commandManager, CommandCategories.GLOBAL,
            // Exclude terraform commands
            entry -> !entry.getKey().startsWith("/")
        ));

        commandManager.register(new HubCommand(server.bridge()));
        commandManager.register(new PlayerInfoCommand(server.playerService(), server.mapService(), server.sessionManager()));

        commandManager.register(new TopTimesCommand(server.mapService(), server.playerService(), server.sessionManager()));

        commandManager.register(new ShowHeightCommand());

        commandManager.register(new SpawnCommand());
        commandManager.register(new SpectateCommand());

        commandManager.register(new FlyCommand());
        commandManager.register(new FlySpeedCommand());
        commandManager.register(new TeleportCommand());
    }

}
