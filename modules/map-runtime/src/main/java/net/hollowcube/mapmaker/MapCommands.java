package net.hollowcube.mapmaker;

import net.hollowcube.command.CommandManager;
import net.hollowcube.command.util.HelpCommand;
import net.hollowcube.common.hud.PlayerHud;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.command.TopTimesCommand;
import net.hollowcube.mapmaker.command.playerinfo.PlayerInfoCommand;
import net.hollowcube.mapmaker.map.command.*;
import net.hollowcube.mapmaker.map.runtime.AbstractMapServer;
import net.hollowcube.mapmaker.runtime.parkour.command.ShowHeightCommand;
import net.hollowcube.mapmaker.runtime.parkour.command.SpectateCommand;
import net.hollowcube.mapmaker.runtime.parkour.replay.ReplayDebugHud;
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
        commandManager.register(new PlayerInfoCommand(server.api(), server.sessionManager()));

        commandManager.register(new TopTimesCommand(server.api(), server.sessionManager()));

        commandManager.register(new ShowHeightCommand());

        commandManager.register(new SpawnCommand());
        commandManager.register(new SpectateCommand());

        commandManager.register(new FlyCommand());
        commandManager.register(new FlySpeedCommand());
        commandManager.register(new TeleportCommand());
    }

    /// Debug subcommands which need types from map-runtime, so [DebugCommand] itself (map-core)
    /// cannot register them. Every server which can host a parkour world calls this from its
    /// `createDebugCommand`.
    public static void registerPlayingDebugSubcommands(@NotNull DebugCommand cmd) {
        cmd.createPermissionedSubcommand(
            "replay",
            (player, _) -> player.scheduleNextTick(
                _ -> PlayerHud.forPlayer(player).toggleModule(new ReplayDebugHud())),
            "Toggles the replay recording overlay"
        );
    }

}
