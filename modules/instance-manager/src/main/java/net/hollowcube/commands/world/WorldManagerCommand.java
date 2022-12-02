package net.hollowcube.commands.world;

import net.minestom.server.command.CommandManager;
import net.minestom.server.command.builder.Command;

public class WorldManagerCommand extends Command {
    /**
     * World manager commands provide the most simple implementation of interacting with world storage in
     * the world management database for a Minestom server instance.
     *
     * This does not deal with tying users to worlds or much other external data.
     *
     * This does not provide any commands to manipulate actual world data, effectively these
     * commands can be thought of as storing any object which undergoes loading, saving,
     * creation, or deletion within a database.
     */
    public WorldManagerCommand() {
        super("worldmanager", "wm");

        addSubcommand(new CopyWorldCommand());
        addSubcommand(new CreateWorldCommand());
        addSubcommand(new DeleteWorldCommand());

        setDefaultExecutor((sender, context) -> sender.sendMessage("TODO add help for cmds"));
    }
}
