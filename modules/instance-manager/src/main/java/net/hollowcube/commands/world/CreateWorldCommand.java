package net.hollowcube.commands.world;

import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;

public class CreateWorldCommand extends Command {
    public CreateWorldCommand() {
        super("create", "c");

        var aliasArg = ArgumentType.String("alias");

        addSyntax((sender, context) -> {
            final String alias = context.get(aliasArg);
            // TODO register world in world manager's database with generated ID and alias
        }, aliasArg);
    }
}
