package net.hollowcube.commands;

import net.hollowcube.commands.world.CreateWorldCommand;
import net.hollowcube.commands.world.WorldManagerCommand;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandManager;

public class CommandLoader {
    public static void registerCommands() {
        CommandManager commandManager = MinecraftServer.getCommandManager();
        commandManager.register(new WorldManagerCommand());
    }
}
