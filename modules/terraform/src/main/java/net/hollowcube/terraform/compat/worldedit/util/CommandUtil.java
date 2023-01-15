package net.hollowcube.terraform.compat.worldedit.util;

import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.condition.CommandCondition;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public final class CommandUtil {
    private CommandUtil() {}

    public static Command singleSyntaxCommand(String name, Consumer<CommandSender> executor, @Nullable CommandCondition commandCondition) {
        var cmd = new Command(name);
        cmd.setCondition(commandCondition);
        cmd.setDefaultExecutor((sender, context) -> executor.accept(sender));
        return cmd;
    }
}
