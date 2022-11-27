package net.hollowcube.terraform.compat.worldedit.util;

import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;

import java.util.function.Consumer;

public final class CommandUtil {
    private CommandUtil() {}

    public static Command singleSyntaxCommand(String name, Consumer<CommandSender> executor) {
        var cmd = new Command(name);
        cmd.setDefaultExecutor((sender, context) -> executor.accept(sender));
        return cmd;
    }
}
