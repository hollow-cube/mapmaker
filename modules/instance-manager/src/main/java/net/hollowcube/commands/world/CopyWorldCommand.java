package net.hollowcube.commands.world;

import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;

public class CopyWorldCommand extends Command {
    public CopyWorldCommand() {
        super("copy", "cp");

        var idArg = ArgumentType.String("id");

        addSyntax((sender, context) -> {
            final String id = context.get(idArg);
            // TODO duplicate the world in world manager's database, do not store locally
            // TODO spit out the new id in console of copied world
        }, idArg);
    }
}
