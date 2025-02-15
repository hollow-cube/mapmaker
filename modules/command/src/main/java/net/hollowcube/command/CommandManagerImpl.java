package net.hollowcube.command;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.util.CommandReflection;
import net.hollowcube.command.util.StringReader;
import net.minestom.server.command.ArgumentParserType;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.play.DeclareCommandsPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.*;
import java.util.function.Consumer;

import static net.minestom.server.network.NetworkBuffer.VAR_INT;

public class CommandManagerImpl implements CommandManager {
    private final CommandManagerImpl parent;
    private final RootCommandNode root = new RootCommandNode();
    private final ReflectionImpl reflection = new ReflectionImpl();

    public CommandManagerImpl() {
        this(null);
    }

    public CommandManagerImpl(@Nullable CommandManager parent) {
        this.parent = (CommandManagerImpl) parent;
    }

    @Override
    public @UnknownNullability CommandNode xpath(@NotNull String path, boolean followRedirects) {
        var result = root.xpath(path, followRedirects);
        if (result != null) return result;
        return parent == null ? null : parent.xpath(path, followRedirects);
    }

    @Override
    public void register(@NotNull String name, @NotNull CommandNode node) {
        root.register(name, node);
    }

    @Override
    public void register(@NotNull String name, @NotNull Consumer<CommandBuilder> func) {
        var builder = new CommandBuilder();
        func.accept(builder);
        root.register(name, builder.node());
    }

    @Override
    public void register(@NotNull CommandDsl command) {
        var builder = new CommandBuilder();
        command.build(builder);
        var node = builder.node();

        register(command.name(), node);
        for (var alias : command.aliases()) {
            register(alias, aliasBuilder -> aliasBuilder.redirect(node));
        }
    }

    @Override
    public @NotNull Suggestion suggest(@NotNull CommandSender sender, @NotNull String input) {
        var reader = new StringReader(input);
        var result = root.suggest(sender, reader);
        if (result.isEmpty() && parent != null) {
            return parent.suggest(sender, input);
//            var parentResult = parent.suggest(sender, input);
//            result.addAll(parentResult);
        }
        return result;
    }

    @Override
    public @NotNull CommandResult execute(@NotNull CommandSender sender, @NotNull String input) {
        var reader = new StringReader(input);
        var context = new CommandContextImpl(sender);
        var result = root.execute(sender, reader, context);
        if (parent != null && result instanceof CommandResult.NotFound)
            return parent.execute(sender, input);
        return result;
    }

    @Override
    public @NotNull DeclareCommandsPacket createCommandPacket(@NotNull Player player) {
        var nodes = new ArrayList<DeclareCommandsPacket.Node>();
        var root = new DeclareCommandsPacket.Node();
        nodes.add(root);

        // Note about redirects:
        // Currently we just tell brigadier that they are a new root literal node which works fine since we never
        // actually send real arguments to the client. However, once we do, they will need to be handled better.

        var rootNodes = new IntArrayList();
        for (var entry : reflect().commands(player, true)) {
            var node = entry.getValue();
            if (node.condition != null) {
                var result = node.condition.test(player, new CommandNode.ConditionContext(player, CommandContext.Pass.BUILD));
                if (result == CommandCondition.HIDE) continue;
            }
            if (node.redirect != null && node.redirect.condition != null) {
                var result = node.redirect.condition.test(player, new CommandNode.ConditionContext(player, CommandContext.Pass.BUILD));
                if (result == CommandCondition.HIDE) continue;
            }

            var args = new DeclareCommandsPacket.Node();
            args.flags = DeclareCommandsPacket.getFlag(DeclareCommandsPacket.NodeType.ARGUMENT, true, false, true);
            args.name = "args";
            args.parser = ArgumentParserType.STRING;
            args.properties = NetworkBuffer.makeArray(buffer -> buffer.write(VAR_INT, 2));
            args.suggestionsType = "minecraft:ask_server";
            nodes.add(args);

            var packetNode = new DeclareCommandsPacket.Node();
            packetNode.flags = DeclareCommandsPacket.getFlag(DeclareCommandsPacket.NodeType.LITERAL, true, false, false);
            packetNode.name = entry.getKey().toLowerCase(Locale.ROOT);
            packetNode.children = new int[]{nodes.size() - 1};
            rootNodes.add(nodes.size());
            nodes.add(packetNode);
        }
        root.children = rootNodes.toIntArray();

        return new DeclareCommandsPacket(nodes, 0);
    }

    @Override
    public @NotNull CommandReflection reflect() {
        return reflection;
    }

    private class ReflectionImpl implements CommandReflection {

        @Override
        public @UnknownNullability CommandNode xpath(@NotNull String path, boolean followRedirects) {
            return root.xpath(path, followRedirects);
        }

        @Override
        public @NotNull Collection<Map.Entry<String, CommandNode>> commands(@NotNull CommandSender sender, boolean includeAliases) {
            var allCommands = new ArrayList<CommandNode.ArgumentPair>();
            if (root.children != null) allCommands.addAll(root.children);
            if (parent != null && parent.root.children != null) allCommands.addAll(parent.root.children);

            var commands = new ArrayList<Map.Entry<String, CommandNode>>();
            for (var pair : allCommands) {
                var node = pair.node();
                if (!includeAliases && node.redirect != null) continue;

                // Test the permissions on the command
                if (node.condition != null) {
                    var result = node.condition.test(sender, new CommandNode.ConditionContext(sender, CommandContext.Pass.SUGGEST));
                    if (result == CommandCondition.HIDE) continue;
                }

                commands.add(Map.entry(pair.argument().id(), node));
            }
            return commands;
        }

        @Override
        public @NotNull Collection<Map.Entry<Argument<?>, CommandNode>> children(@NotNull CommandNode node, @NotNull CommandSender sender) {
            if (node.children == null) return List.of();
            var commands = new ArrayList<Map.Entry<Argument<?>, CommandNode>>();
            for (var pair : node.children) {
                var child = pair.node();

                // Test the permissions on the command
                if (child.condition != null) {
                    var result = child.condition.test(sender, new CommandNode.ConditionContext(sender, CommandContext.Pass.SUGGEST));
                    if (result == CommandCondition.HIDE) continue;
                }

                commands.add(Map.entry(pair.argument(), child));
            }
            return commands;
        }
    }
}
