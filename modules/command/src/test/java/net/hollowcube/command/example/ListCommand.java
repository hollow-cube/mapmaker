package net.hollowcube.command.example;

import net.hollowcube.command.Command;
import net.hollowcube.command.CommandCondition;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

// Should support the following syntaxes:
// /list
// /list [target] - only for console sender
public class ListCommand extends Command {
    private final Argument<String> targetArg = Argument.Word("target")
            .with("notmattw", "SethPRG");

    public ListCommand() {
        super("list");

        var condition = (CommandCondition) (sender, context) -> sender instanceof Player ? CommandCondition.HIDE : CommandCondition.ALLOW;
        addSyntax(condition, this::execute, targetArg);
        setDefaultExecutor(this::execute);
    }

    private void execute(@NotNull CommandSender sender, @NotNull CommandContext context) {

    }
}
