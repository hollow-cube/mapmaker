package net.hollowcube.command;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.minestom.server.command.CommandSender;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.play.DeclareCommandsPacket;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static net.minestom.server.network.NetworkBuffer.VAR_INT;

/**
 * A command manager is a hierarchical structure for holding commands.
 *
 * <p>It is possible to have one command manager, or nest them arbitrarily.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class CommandManager {
    //todo hierarchy

    // Lock must be held for any modification to commands.
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock rlock = lock.readLock();
    private final Lock wlock = lock.writeLock();

    // Map of lower case command name to command impl.
    private final Map<String, Command> commands = new HashMap<>();
    private final List<Command> uniqueCommands = new ArrayList<>();

    /**
     * Registers the given command.
     *
     * @param command The command to register
     */
    public void register(@NotNull Command command) {
        wlock.lock();
        try {
            var name = command.name().toLowerCase(Locale.ROOT);
            Check.argCondition(name.isEmpty(), "Command name cannot be empty.");
            Check.argCondition(commands.containsKey(name), "Command already registered: " + name);
            for (var alias : command.aliases()) {
                Check.argCondition(alias.isEmpty(), "Command alias cannot be empty.");
                Check.argCondition(commands.containsKey(alias.toLowerCase(Locale.ROOT)), "Command already registered: " + alias);
            }

            commands.put(name, command);
            for (var alias : command.aliases())
                commands.put(alias.toLowerCase(Locale.ROOT), command);
            uniqueCommands.add(command);
        } finally {
            wlock.unlock();
        }
    }

    public @NotNull Map<String, Command> getCommands() {
        return Map.copyOf(commands);
    }

    public @NotNull List<Command> getUniqueCommands() {
        return List.copyOf(uniqueCommands);
    }

    /**
     * Suggestions returns the command suggestions for the given inputs, assuming the cursor is at the end of the input.
     *
     * @param sender The sender to get suggestions for
     * @param input  The text input from the sender, assuming the cursor is at the end
     * @return Suggestions for the given input from the player
     */
    public @NotNull Suggestion suggestions(@NotNull CommandSender sender, @NotNull String input) {
        var context = expand(CommandContext.Pass.SUGGEST, sender, input);
        if (context.isFailed()) return Suggestion.EMPTY;

        var lastArg = context.resetToLastArg();
        if (lastArg == null) return Suggestion.EMPTY;

        var result = new Suggestion(context.reader().pos(), context.reader().remaining());
        lastArg.suggestions(sender, context.reader(), result);
        return result;
    }

    /**
     * Executes the command with the given input.
     *
     * @param sender The sender to execute the command as
     * @param input  The text input from the sender
     */
    public void execute(@NotNull CommandSender sender, @NotNull String input) {
        var context = expand(CommandContext.Pass.EXECUTE, sender, input);
        if (!context.hasCommand()) {
            sender.sendMessage("no such command"); //todo
            return;
        }
        if (context.isFailed()) return; //todo, allow handling? log? something?
        context.executor().execute(sender, context);
    }

    @NotNull CommandContextImpl expand(@NotNull CommandContext.Pass pass, @NotNull CommandSender sender, @NotNull String input) {
        var reader = new StringReader(input);
        var context = new CommandContextImpl(pass, sender, reader);

        var commandName = reader.readWord(WordType.ALPHANUMERIC).toLowerCase(Locale.ROOT);
        var command = commands.get(commandName);
        return command == null ? context : expand(context, command);
    }

    private @NotNull CommandContextImpl expand(@NotNull CommandContextImpl context, @NotNull Command command) {
        var commandCondition = command.condition();
        if (commandCondition != null && commandCondition.test(context.sender(), context) == CommandCondition.HIDE) {
            //todo support DENY
            return context;
        }

        context.pushCommand(command);
        var reader = context.reader();
        var sender = context.sender();

        // Try to match a subcommand
        var scmark = reader.mark();
        var subcommandName = reader.readWord(WordType.ALPHANUMERIC).toLowerCase(Locale.ROOT);
        var subcommand = command.findSubcommand(subcommandName);
        if (subcommand != null) return expand(context, subcommand);
        reader.restore(scmark);

        // Try to match an argument
        syntaxes:
        for (var syntax : command.syntaxes()) {
            var cmark = context.mark();
            context.pushSyntax(syntax);

            // If the condition is a hide, we should skip this syntax.
            if (syntax.condition() != null && syntax.condition().test(sender, context) == CommandCondition.HIDE) {
                //todo support DENY
                context.restore(cmark);
                continue;
            }

            if (!reader.canRead() && !syntax.allowsEmpty()) {
                context.restore(cmark);
                continue;
            }

            for (var arg : syntax.args()) {

                // If we reached end of input, we can skip optional arguments.
                if (!reader.canRead()) {
                    if (arg.isOptional() && context.pass() == CommandContext.Pass.EXECUTE) {
                        context.pushArg(arg);
                        context.pushArgValue("", arg.getDefaultValue(sender), null);
                    }

                    continue;
                }

                context.pushArg(arg);

                var argMark = reader.mark();
                switch (arg.parse(context.sender(), context.reader())) {
                    case Argument.ParseSuccess<?> success -> {
                        // If we hit a success, store the value and continue to the next argument.
                        context.pushArgValue(reader.rawSince(argMark), success.value(), null);
                    }
                    case Argument.ParseDeferredSuccess<?> deferred -> {
                        context.pushArgValue(reader.rawSince(argMark), deferred.value(), null);
                    }
                    case Argument.ParseFailure<?> ignored -> {

                        var errorHandler = arg.errorHandler();
                        if (errorHandler != null) {
                            context.pushArgValue(reader.rawSince(argMark), null, null);
                            // If there is an error handler, this syntax can be valid.
                            context.setOverrideExecutor(errorHandler);
                        } else {

                            // If the argument is optional, we can try to skip it and continue to the next argument after it.
                            if (arg.isOptional()) {
                                context.pushArgValue(reader.rawSince(argMark), null, null);
                                reader.restore(argMark);
                                continue;
                            }

                            // This tree is dead, and we should move to the next syntax
                            context.restore(cmark);
                            continue syntaxes;
                        }
                    }
                    case Argument.ParsePartial<?> ignored -> {
                        // If we hit a partial match, we must either be at the end or this is irrelevant (???)
                        context.pushArgValue(reader.rawSince(argMark), null, arg.errorHandler());
                    }
                }
            }

            // If we made it through all the syntaxes and there is extra input, then we failed to match.
            if (reader.canRead()) {
                context.restore(cmark);
                continue;
            }

            return context;
        }

        return context;
    }

    /**
     * Returns the {@link DeclareCommandsPacket} for the given player.
     */
    public @NotNull DeclareCommandsPacket commandPacket(@NotNull CommandSender sender) {
        var nodes = new ArrayList<DeclareCommandsPacket.Node>();
        var root = new DeclareCommandsPacket.Node();
        nodes.add(root);

        var rootNodes = new IntArrayList();
        for (var command : uniqueCommands) {
            rootNodes.addAll(buildCommandPacket(sender, nodes, command));
        }

        root.children = rootNodes.toIntArray();
        return new DeclareCommandsPacket(nodes, 0);
        //todo caching should be as aggressive as possible. For example, a server only command should be cached globally here because it is always single syntax greedy string.
    }

    private IntList buildCommandPacket(@Nullable CommandSender sender, @NotNull List<DeclareCommandsPacket.Node> nodes, @NotNull Command command) {
        var cmds = new IntArrayList();

        // Eval the condition first to see if we should even continue
        var commandCondition = command.condition();
        if (commandCondition != null && commandCondition.test(sender, null) == CommandCondition.HIDE) {
            return cmds;
        }

        // Add the command node
        var node = new DeclareCommandsPacket.Node();
        node.flags = DeclareCommandsPacket.getFlag(DeclareCommandsPacket.NodeType.LITERAL, true, false, false);
        node.name = command.name().toLowerCase(Locale.ROOT);
        cmds.add(nodes.size());
        nodes.add(node);
        var nodeChildren = new IntArrayList();

        // Add subcommands to the command node
        for (var subcommand : command.getUniqueSubcommands()) {
            nodeChildren.addAll(buildCommandPacket(sender, nodes, subcommand));
        }

        // Add the greedy argument to the command node if there are any syntaxes or a default executor
        if (command.isPlausiblyExecutable()) {
            var args = new DeclareCommandsPacket.Node();
            args.flags = DeclareCommandsPacket.getFlag(DeclareCommandsPacket.NodeType.ARGUMENT, true, false, true);
            args.name = "args";
            args.parser = "brigadier:string";
            args.properties = NetworkBuffer.makeArray(buffer -> buffer.write(VAR_INT, 2));
            args.suggestionsType = "minecraft:ask_server";
            nodeChildren.add(nodes.size());
            nodes.add(args);
        }

        node.children = nodeChildren.toIntArray();

        // Add redirects for aliases to the command
        for (var alias : command.aliases()) {
            var redirect = new DeclareCommandsPacket.Node();
            redirect.flags = DeclareCommandsPacket.getFlag(DeclareCommandsPacket.NodeType.LITERAL, false, true, false);
            redirect.name = alias.toLowerCase(Locale.ROOT);
            redirect.redirectedNode = cmds.getInt(0);
            cmds.add(nodes.size());
            nodes.add(redirect);
        }
        //todo

        return cmds;
    }

}
