package net.hollowcube.mapmaker.dev.command;

import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class TestCommand extends Command {
    private static final Argument<String[]> allArgsArg = ArgumentType.StringArray("args")
            .setSuggestionCallback((sender, context, suggestion) -> {

                System.out.println("input: '" + suggestion.getInput() + "', start: " + suggestion.getStart());
//                suggestion.setStart();

                suggestion.addEntry(new SuggestionEntry("test1"));
                suggestion.addEntry(new SuggestionEntry("test2"));
            });

    public TestCommand() {
        super("test");

        addSyntax(this::execute, allArgsArg);
    }

    private void execute(@NotNull CommandSender sender, @NotNull CommandContext context) {
        var args = context.get(allArgsArg);
        sender.sendMessage("Test command executed! " + Arrays.toString(args));
    }
}
