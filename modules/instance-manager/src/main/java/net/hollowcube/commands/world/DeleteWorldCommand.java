package net.hollowcube.commands.world;

import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;

public class DeleteWorldCommand extends Command {
    public DeleteWorldCommand() {
        super("delete" , "d");

        var idArg = ArgumentType.String("id");

        addSyntax((sender, context) -> {
            final String id = context.get(idArg);
            // TODO remove world from world manager's database by ID
        }, idArg);
    }
}
