package net.hollowcube.command;

import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ArgumentInt;
import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HelpCommand extends Command {
    private final CommandManager commandManager;
    private final CommandDoc.DocRenderer renderer;

    private final ArgumentInt pageArg = Argument.Int("page").min(0);
    private final Argument<ResolvedCommand> commandArg = Argument.GreedyString("command")
            .map(this::resolveCommand, this::suggestCommand)
            .errorHandler(this::handleUnknownCommand)
            .doc("The command to show help for");

    public HelpCommand(@NotNull CommandManager commandManager) {
        this(commandManager, CommandDoc.defaultRenderer());
    }

    public HelpCommand(@NotNull CommandManager commandManager, @NotNull CommandDoc.DocRenderer renderer) {
        super("help");
        this.commandManager = commandManager;
        this.renderer = renderer;

        description = "Lists commands or shows help for a command.";
        examples = List.of("/help", "/help tp");

        // Command list (with paging support)
        setDefaultExecutor(this::showCommandList);
        addSyntax(CommandCondition.nosuggestion(), this::showCommandList, pageArg);

        // Command help by name
        addSyntax(this::showCommandHelp, commandArg);
    }

    private void showCommandList(@NotNull CommandSender sender, @NotNull CommandContext context) {
        sender.sendMessage("available commands:");
        for (var command : commandManager.getUniqueCommands()) {
            if (!testCondition(command, sender)) continue;

            sender.sendMessage(command.name());
        }
    }

    private void showCommandHelp(@NotNull CommandSender sender, @NotNull CommandContext context) {
        var resolved = context.get(commandArg);
        var fullPath = new ArrayList<>(resolved.path);
        fullPath.add(resolved.command.name());
        renderer.render(sender, fullPath, resolved.command.doc(sender));
    }

    record ResolvedCommand(@NotNull List<String> path, @NotNull Command command) {
    }

    @NotNull Argument.ParseResult<@Nullable ResolvedCommand> resolveCommand(@NotNull CommandSender sender, @NotNull String raw) {
        if (raw.isEmpty()) return new Argument.ParsePartial<>();

        var path = new ArrayList<String>();

        var reader = new StringReader(raw);
        Command command = null;
        String word = "";
        while (reader.canRead()) {
            word = reader.readWord(WordType.ALPHANUMERIC).toLowerCase(Locale.ROOT);
            var children = command == null ? commandManager.getCommands() : command.getSubcommands();
            if (reader.canRead()) {
                // If there is more input, this must match a command exactly
                command = children.get(word);
                if (command == null || !testCondition(command, sender))
                    return new Argument.ParseFailure<>();
                path.add(command.name());
            } else {
                // We are at the end, so we must match a subcommand
                boolean isPartial = false;
                for (var entry : children.entrySet()) {
                    if (!testCondition(entry.getValue(), sender)) continue;

                    if (entry.getKey().equals(word)) {
                        // Exact match, we are done.
                        return new Argument.ParseSuccess<>(new ResolvedCommand(path, entry.getValue()));
                    }
                    isPartial |= entry.getKey().startsWith(word);
                }
                if (isPartial) return new Argument.ParsePartial<>();
                return new Argument.ParseFailure<>();
            }
        }

        return new Argument.ParseFailure<>();
    }

    void suggestCommand(@NotNull CommandSender sender, @NotNull StringReader rawReader, @NotNull Suggestion suggestion, @NotNull String raw) {
        // If there is no input, blanket suggest all commands
        if (raw.isEmpty()) {
            for (var entry : commandManager.getCommands().entrySet()) {
                if (!testCondition(entry.getValue(), sender)) continue;
                suggestion.add(entry.getKey());
            }
            return;
        }

        // Otherwise, walk through the input to find a subcommand
        var reader = new StringReader(raw);
        Command command = null;
        String word = "";
        while (reader.canRead()) {
            word = reader.readWord(WordType.ALPHANUMERIC).toLowerCase(Locale.ROOT);
            var children = command == null ? commandManager.getCommands() : command.getSubcommands();
            if (reader.canRead()) {
                // If there is more input, this must match a command exactly
                command = children.get(word);
                if (!testCondition(command, sender)) command = null;
            } else {
                // We are at the end, so we can suggest commands.
                for (var entry : children.entrySet()) {
                    if (!testCondition(entry.getValue(), sender)) continue;

                    if (entry.getKey().startsWith(word)) {
                        suggestion.add(entry.getKey());
                    }
                }
                break;
            }

            // If we did not match a command, return no suggestions
            if (command == null) {
                suggestion.clear();
                return;
            }
        }
    }

    void handleUnknownCommand(@NotNull CommandSender sender, @NotNull CommandContext context) {
        sender.sendMessage(Component.translatable("command.help.unknown_command", Component.text(context.getRaw(commandArg))));
    }

    private boolean testCondition(@NotNull Command command, @NotNull CommandSender sender) {
        var condition = command.condition();
        if (condition == null) return true;

        var eval = condition.test(sender, CommandContext.fake(sender));
        return eval == CommandCondition.ALLOW;
    }

}
