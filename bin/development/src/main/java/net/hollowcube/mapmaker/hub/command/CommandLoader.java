package net.hollowcube.mapmaker.hub.command;

import net.hollowcube.server.commands.debug.StopCommand;
import net.hollowcube.server.commands.debug.TestCommand;
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
