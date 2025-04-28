package net.hollowcube.command.util;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.CommandManager;
import net.hollowcube.command.CommandNode;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ArgumentLiteral;
import net.hollowcube.command.arg.ParseResult;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.common.util.FontUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.command.ArgumentParserType;
import net.minestom.server.command.CommandSender;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public class HelpCommand extends CommandDsl {
    private static final TextColor DARK_GRAY = TextColor.color(0x696969);
    private static final TextColor WHITE_GRAY = TextColor.color(0xF2F2F2);

    private final Argument<ResolvedCommand> commandArg = new ArgumentCommand()
            .description("The command to show help for");

    private final CommandReflection reflect;
    private final Predicate<Map.Entry<String, CommandNode>> filter;

    public HelpCommand(@NotNull CommandManager commandManager, @Nullable CommandCategory category) {
        this("help", new String[]{"h"}, commandManager, category, $ -> true);
    }

    /**
     * @param name
     * @param aliases
     * @param commandManager
     * @param category
     * @param filter         A filter to test top level commands (it will never run on subcommands)
     */
    public HelpCommand(
            @NotNull String name, @NotNull String[] aliases,
            @NotNull CommandManager commandManager,
            @Nullable CommandCategory category,
            @NotNull Predicate<Map.Entry<String, CommandNode>> filter
    ) {
        super(name, aliases);
        this.reflect = commandManager.reflect();
        this.filter = filter;

        this.description = "Shows a list of commands and detailed help for each";
        this.category = category;
        this.examples = List.of("/help", "/help tp");

        addSyntax(this::handleShowCommandList);
        addSyntax(this::handleShowCommandDetail, commandArg);
    }

    private void handleShowCommandList(@NotNull CommandSender sender, @NotNull CommandContext context) {
        var rootCommands = new ArrayList<>(reflect.commands(sender, false));
        if (rootCommands.isEmpty()) { // Sanity
            sender.sendMessage(Component.text("No commands available"));
            return;
        }
        // Remove the commands that don't match the filter
        rootCommands.removeIf(entry -> !filter.test(entry));

        // Compute the width of the biggest command, and group by category
        var byCategory = new TreeMap<CommandCategory, List<Map.Entry<String, CommandNode>>>(Comparator.comparingInt(CommandCategory::order));
        int maxCommandLength = 0;
        for (var entry : rootCommands) {
            var category = entry.getValue().category();
            if (category == null) continue;

            var commandName = entry.getKey();
            maxCommandLength = Math.max(maxCommandLength, FontUtil.measureText("/" + commandName));
            byCategory.computeIfAbsent(entry.getValue().category(), $ -> new ArrayList<>()).add(entry);
        }

        // Create the message
        boolean first = true;
        var message = Component.text();
        for (var entry : byCategory.sequencedEntrySet()) {
            var category = entry.getKey();
            if (entry.getValue().isEmpty()) continue;

            if (!first) message.appendNewline();
            first = false;

            message.append(Component.text(category.displayName()));

            var commands = entry.getValue();
            for (int i = 0; i < commands.size(); i += 2) {
                var commandEntry = commands.get(i);
                var commandName = "/" + commandEntry.getKey();

                message.appendNewline().append(Component.text(commandName)
                        .hoverEvent(HoverEvent.showText(createDetail(List.of(commandEntry.getKey()), commandEntry.getValue(), sender))));

                if (i + 1 < commands.size()) {
                    var nextCommandEntry = commands.get(i + 1);
                    var nextCommandName = "/" + nextCommandEntry.getKey();
                    message.append(Component.text(FontUtil.computeOffset(maxCommandLength - FontUtil.measureText(commandName) + 10))
                            .append(Component.text(nextCommandName)
                                    .hoverEvent(HoverEvent.showText(createDetail(List.of(nextCommandEntry.getKey()), nextCommandEntry.getValue(), sender)))));
                }

            }
        }

        sender.sendMessage(message.build());
    }

    private void handleShowCommandDetail(@NotNull CommandSender sender, @NotNull CommandContext context) {
        var resolved = context.get(commandArg);
        var command = resolved.node;

        // We allow aliases in show detail so need to resolve the command if it is.
        var redirect = command.redirect();
        if (redirect != null) command = redirect;

        sender.sendMessage(createDetail(resolved.path, command, sender));
    }

    private @NotNull Component createDetail(@NotNull List<Object> path, @NotNull CommandNode node, @NotNull CommandSender sender) {
        var args = new ArrayList<List<Map.Entry<Argument<?>, CommandNode>>>();
        collectChildren(args, node, sender, path.size());

        var builder = Component.text();

        builder.append(Component.text(Objects.requireNonNullElse(node.description(), "description.missing"))).appendNewline();

        List<Argument<?>> pathArgs = new ArrayList<>();
        TextComponent.Builder usageString = Component.text();
        for (var part : path) {
            if (part instanceof Argument<?> arg) {
                pathArgs.add(arg);
                usageString.append(Component.text("<", DARK_GRAY))
                        .append(Component.text(arg.id(), WHITE_GRAY))
                        .append(Component.text("> ", DARK_GRAY));
            } else {
                usageString.append(Component.text(part + " ", WHITE_GRAY));
            }
        }

        // Build the usage string as well as the subcommands and arguments subtexts.
        // An argument is considered a subcommand if it is a literal, arg otherwise.
        boolean hasArgs = false, hasSubcommands = false;
        TextComponent.Builder argsMessage = Component.text(), subcommandsMessage = Component.text();
        builder.append(Component.text("ᴜѕᴀɢᴇ: /").append(usageString));
        for (var arg : pathArgs) {
            argsMessage.appendNewline().appendSpace().append(Component.text(arg.id()))
                    .append(Component.text(": ", DARK_GRAY))
                    .append(Component.text(Objects.requireNonNullElse(arg.description(), "description.missing")));
            hasArgs = true;
        }
        for (var arg : args) {
            builder.append(Component.text("<", DARK_GRAY));

            var iter = arg.iterator();
            while (iter.hasNext()) {
                var entry = iter.next();
                var argument = entry.getKey();
                // Append to usage string
                builder.append(Component.text(argument.id(), WHITE_GRAY));
                // Append to commands or subcommands
                if (argument instanceof ArgumentLiteral) {
                    subcommandsMessage.appendNewline().appendSpace().append(Component.text(argument.id()))
                            .append(Component.text(": ", DARK_GRAY))
                            .append(Component.text(Objects.requireNonNullElse(entry.getValue().description(), "description.missing")));
                    hasSubcommands = true;
                } else {
                    argsMessage.appendNewline().appendSpace().append(Component.text(argument.id()))
                            .append(Component.text(": ", DARK_GRAY))
                            .append(Component.text(Objects.requireNonNullElse(argument.description(), "description.missing")));
                    hasArgs = true;
                }

                if (iter.hasNext()) {
                    builder.append(Component.text("|", DARK_GRAY));
                }
            }

            builder.append(Component.text(">", DARK_GRAY).appendSpace());
        }

        // Args & Subcommands sections
        if (hasArgs) {
            builder.appendNewline().append(Component.text("ᴀʀɢᴜᴍᴇɴᴛѕ:"));

            builder.append(argsMessage);
        }
        if (hasSubcommands) {
            builder.appendNewline().append(Component.text("ѕᴜʙᴄᴏᴍᴍᴀɴᴅѕ:"));
            builder.append(subcommandsMessage);
        }

        // Examples
        var examples = node.examples();
        if (examples != null && !examples.isEmpty()) {
            builder.appendNewline().append(Component.text("ᴇxᴀᴍᴘʟᴇѕ:"));
            for (var example : examples) {
                builder.appendNewline().append(Component.text(example));
            }
        }

        return builder.build();
    }

    private void collectChildren(@NotNull List<List<Map.Entry<Argument<?>, CommandNode>>> args, @NotNull CommandNode node, @NotNull CommandSender sender, int depth) {
        var children = reflect.children(node, sender);
        if (children.isEmpty()) return;

        List<Map.Entry<Argument<?>, CommandNode>> list;
        if (args.size() <= depth) {
            list = new ArrayList<>();
            args.add(list);
        } else {
            list = args.get(depth);
        }

        // If there is only one child it should be added, and we can inspect those children
        if (children.size() == 1) {
            var entry = children.iterator().next();
            list.add(entry);
            collectChildren(args, entry.getValue(), sender, depth + 1);
            return;
        }

        // Otherwise add all children but do not go any deeper.
        list.addAll(children);
    }

    private record ResolvedCommand(
            // Entries are ALWAYS a string or Argument<?>. Not just Argument<?>s
            // because we dont have arguments at the root, only strings.
            @NotNull List<Object> path,
            @NotNull CommandNode node
    ) {
    }

    private class ArgumentCommand extends Argument<ResolvedCommand> {

        private ArgumentCommand() {
            super("command");
        }

        @Override
        public void properties(NetworkBuffer buffer) {
            // should probably be changed to be literals and not just greedy string but im too lazy to change the whole command
            buffer.write(NetworkBuffer.VAR_INT, 2);
        }

        @Override
        public ArgumentParserType argumentType() {
            return ArgumentParserType.STRING;
        }

        @Override
        public @NotNull ParseResult<ResolvedCommand> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
            reader = new StringReader(reader.readRemaining());
            var word = reader.readWord(WordType.GREEDY).toLowerCase(Locale.ROOT);

            CommandNode target = null;
            List<Object> path = new ArrayList<>();
            boolean maybePartial = false;
            for (var entry : reflect.commands(sender, true)) {
                if (!filter.test(entry)) continue;

                var commandName = entry.getKey();
                if (commandName.equals(word)) {
                    target = entry.getValue();
                    path.add(commandName);
                    break;
                } else if (commandName.startsWith(word)) {
                    maybePartial = true;
                }
            }

            // Try to match a subcommand as long as there is more input and we matched a previous command
            subcommands:
            while (target != null && reader.canRead()) {
                word = reader.readWord(WordType.GREEDY).toLowerCase(Locale.ROOT);

                maybePartial = false;
                for (var entry : collectPossibleNext(path, target, sender)) {
                    var argumentId = entry.getKey().id().toLowerCase(Locale.ROOT);
                    //todo match only literals
                    if (argumentId.equals(word)) {
                        target = entry.getValue();
                        path.add(entry.getKey() instanceof ArgumentLiteral lit ? lit.id() : entry.getKey());
                        continue subcommands;
                    } else if (argumentId.startsWith(word)) {
                        maybePartial = true;
                    }
                }

                // if we didn't match a subcommand remove the parent
                target = null;
            }

            if (target != null)
                return new ParseResult.Success<>(new ResolvedCommand(path, target));
            return !reader.canRead() && maybePartial ? new ParseResult.Partial<>() : new ParseResult.Failure<>(-1);
        }

        @Override
        public void suggest(@NotNull CommandSender sender, @NotNull String raw, @NotNull Suggestion suggestion) {
            // This is definitely a hack.
            // Basically we get a trimmed version of raw but need to know if there is a space at the end (which should suggest the next command)
            // So we look if the suggestion length (which goes to the end) is longer than the raw length, and if so we add a space.
            if (suggestion.getLength() > raw.length()) {
                raw += " ";
            }

            // If empty always suggest commands without aliases
            if (raw.isEmpty()) {
                for (var entry : reflect.commands(sender, false)) {
                    if (!filter.test(entry)) continue;
                    suggestion.add(entry.getKey());
                }
                return;
            }

            var reader = new StringReader(raw);
            var word = reader.readWord(WordType.GREEDY).toLowerCase(Locale.ROOT);

            CommandNode target = null;
            for (var entry : reflect.commands(sender, true)) {
                if (!filter.test(entry)) continue;

                var commandName = entry.getKey();
                if (commandName.equals(word)) {
                    target = entry.getValue();
                    break;
                } else if (commandName.startsWith(word)) {
                    suggestion.add(commandName);
                }
            }

            // Try to match a subcommand as long as there is more input and we matched a previous command
            subcommands:
            while (target != null && reader.canRead()) {
                word = reader.readWord(WordType.GREEDY).toLowerCase(Locale.ROOT);

                for (var entry : collectPossibleNext(new ArrayList<>(), target, sender)) {
                    var argumentId = entry.getKey().id().toLowerCase(Locale.ROOT);
                    //todo match only literals
                    if (argumentId.equals(word)) {
                        target = entry.getValue();
                        continue subcommands;
                    }
                }

                // No match, if we are at the last word thats fine. otherwise its a fail
                if (reader.canRead()) return;
            }

            if (target == null) return;

            // Try to suggest on the end
            suggestion.setStart(suggestion.getStart() + reader.pos() - word.length());
            suggestion.setLength(word.length());
            for (var entry : collectPossibleNext(null, target, sender)) {
                var argId = entry.getKey().id();
                if (argId.toLowerCase(Locale.ROOT).startsWith(word))
                    suggestion.add(argId);
            }
        }

        private @NotNull Collection<Map.Entry<Argument<?>, CommandNode>> collectPossibleNext(@Nullable List<Object> path, @NotNull CommandNode target, @NotNull CommandSender sender) {
            var children = reflect.children(target, sender);
            if (children.isEmpty()) return List.of();

            var results = new ArrayList<Map.Entry<Argument<?>, CommandNode>>();
            for (var entry : children) {
                var argument = entry.getKey();
                if (argument instanceof ArgumentLiteral) {
                    results.add(entry);
                } else {
                    if (path != null) path.add(argument);
                    results.addAll(collectPossibleNext(path, entry.getValue(), sender));
                }
            }

            return results;
        }
    }

}
