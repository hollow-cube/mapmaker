package net.hollowcube.server.commands;

import net.hollowcube.server.commands.debug.StopCommand;
import net.hollowcube.server.commands.debug.TestCommand;
import net.hollowcube.server.commands.map.CreateMapCommand;
import net.hollowcube.server.commands.map.JoinMapCommand;
import net.hollowcube.server.commands.map.LeaveMapCommand;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandManager;

public class CommandLoader {

    public void registerCommands() {
        CommandManager manager = MinecraftServer.getCommandManager();
        manager.register(new TestCommand());
        manager.register(new StopCommand());

        manager.register(new CreateMapCommand());
        manager.register(new JoinMapCommand());
        manager.register(new LeaveMapCommand());
    }
}
