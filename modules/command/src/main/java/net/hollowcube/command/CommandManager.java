package net.hollowcube.command;

import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ArgumentOptional;
import net.hollowcube.command.arg.SuggestionResult;
import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.minestom.server.command.CommandSender;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.play.DeclareCommandsPacket;
import org.jetbrains.annotations.NotNull;

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

    /**
     * Registers the given command.
     *
     * @param command The command to register
     */
    public void register(@NotNull Command command) {
        wlock.lock();
        try {
            var name = command.name().toLowerCase(Locale.ROOT);
            if (commands.containsKey(name))
                throw new IllegalArgumentException("Command already registered: " + name);
            commands.put(name, command);
        } finally {
            wlock.unlock();
        }
    }

    public @NotNull Map<String, Command> getCommands() {
        return Map.copyOf(commands);
    }

    /**
     * Suggestions returns the command suggestions for the given inputs, assuming the cursor is at the end of the input.
     *
     * @param sender The sender to get suggestions for
     * @param input  The text input from the sender, assuming the cursor is at the end
     * @return Suggestions for the given input from the player
     */
    public @NotNull SuggestionResult suggestions(@NotNull CommandSender sender, @NotNull String input) {
        var context = expand(CommandContext.Pass.SUGGEST, sender, input);
        if (context.isFailed()) return new SuggestionResult.Failure();

        var lastArg = context.resetToLastArg();
        if (lastArg == null) return new SuggestionResult.Success(0, 0, List.of());

        return lastArg.suggestions(sender, context.reader());
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
                if (!reader.canRead() && arg instanceof ArgumentOptional<?>) {
                    continue;
                }

                context.pushArg(arg);

                var argMark = reader.mark();
                switch (arg.parse(context.sender(), context.reader())) {
                    case Argument.ParseSuccess<?> success -> {
                        // If we hit a success, store the value and continue to the next argument.
                        context.pushArgValue(success.value(), null);
                    }
                    case Argument.ParseFailure<?> ignored -> {

                        var errorHandler = arg.errorHandler();
                        if (errorHandler != null) {
                            // If there is an error handler, this syntax can be valid.
                            context.setOverrideExecutor(errorHandler);
                        } else {

                            // If the argument is optional, we can try to skip it and continue to the next argument after it.
                            if (arg instanceof ArgumentOptional<?>) {
                                context.pushArgValue(null, null);
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
                        context.pushArgValue(null, arg.errorHandler());
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
        //todo caching should be as aggressive as possible. For example, a server only command should be cached globally here because it is always single syntax greedy string.
        var nodes = new ArrayList<DeclareCommandsPacket.Node>();
        var topLevel = new ArrayList<Integer>();
        for (var command : commands.values()) {
            var node = new DeclareCommandsPacket.Node();
            node.flags = DeclareCommandsPacket.getFlag(DeclareCommandsPacket.NodeType.LITERAL, true, false, false);
            node.name = command.name().toLowerCase(Locale.ROOT);
            node.children = new int[1];
            topLevel.add(nodes.size());
            nodes.add(node);

            // Args greedy argumet
            var args = new DeclareCommandsPacket.Node();
            args.flags = DeclareCommandsPacket.getFlag(DeclareCommandsPacket.NodeType.ARGUMENT, true, false, true);
            args.name = "args";
            args.parser = "brigadier:string";
            args.properties = NetworkBuffer.makeArray(buffer -> buffer.write(VAR_INT, 2));
            args.suggestionsType = "minecraft:ask_server";
            node.children[0] = nodes.size();
            nodes.add(args);
        }

        var rootNode = new DeclareCommandsPacket.Node();
        rootNode.children = topLevel.stream().mapToInt(i -> i).toArray();
        nodes.add(rootNode);

        return new DeclareCommandsPacket(nodes, nodes.size() - 1);
    }

}
