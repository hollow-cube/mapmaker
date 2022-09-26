package net.hollowcube.server.commands.debug;

import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;

public class TestCommand extends Command {
    public TestCommand() {
        super("test");

        setDefaultExecutor((sender, context) -> {
            sender.sendMessage("Usage: /test <input-to-echo>");
        });

        var echoArg = ArgumentType.String("echo");

        addSyntax((sender, context) -> {
            final String echo = context.get(echoArg);
            sender.sendMessage(echo);
        }, echoArg);
    }
}
