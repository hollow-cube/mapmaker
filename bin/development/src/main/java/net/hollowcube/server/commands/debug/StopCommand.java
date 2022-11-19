package net.hollowcube.server.commands.debug;

import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;

public class StopCommand extends Command {
    public StopCommand() {
        super("stop", "shutdown");

        setDefaultExecutor((sender, context) -> MinecraftServer.stopCleanly());
    }
}
