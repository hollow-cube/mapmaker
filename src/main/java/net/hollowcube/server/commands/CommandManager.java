package net.hollowcube.server.commands;

import net.minestom.server.MinecraftServer;
import omega.mapmaker.commands.debug.TestCommand;
import omega.mapmaker.commands.map.CreateMapCommand;
import omega.mapmaker.commands.map.JoinMapCommand;
import omega.mapmaker.commands.map.LeaveMapCommand;

public class CommandManager {
    private static final net.minestom.server.command.CommandManager manager = MinecraftServer.getCommandManager();

    public void registerCommands() {
        manager.register(new TestCommand());

        manager.register(new CreateMapCommand());
        manager.register(new JoinMapCommand());
        manager.register(new LeaveMapCommand());
    }
}
