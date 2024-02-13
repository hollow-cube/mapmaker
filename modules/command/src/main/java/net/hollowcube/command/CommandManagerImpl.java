package net.hollowcube.command;

import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.util.CommandReflection;
import net.hollowcube.command.util.StringReader;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.DeclareCommandsPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class CommandManagerImpl implements CommandManager {
    private final RootCommandNode root = new RootCommandNode();
    private final ReflectionImpl reflection = new ReflectionImpl();

    @Override
    public @UnknownNullability CommandNode xpath(@NotNull String path, boolean followRedirects) {
        return root.xpath(path, followRedirects);
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
        return root.suggest(sender, reader);
    }

    @Override
    public @NotNull CommandResult execute(@NotNull CommandSender sender, @NotNull String input) {
        var reader = new StringReader(input);
        var context = new CommandContextImpl(sender);
        return root.execute(sender, reader, context);
    }

    @Override
    public @Nullable DeclareCommandsPacket createCommandPacket(@NotNull Player player) {
        return root.createCommandPacket(player);
    }

    @Override
    public @NotNull CommandReflection reflect() {
        return reflection;
    }

    private class ReflectionImpl implements CommandReflection {

        @Override
        public @NotNull Collection<Map.Entry<String, CommandNode>> commands(@NotNull CommandSender sender, boolean includeAliases) {
            var commands = new ArrayList<Map.Entry<String, CommandNode>>();
            for (CommandNode.ArgumentPair(Argument<?> argument, CommandNode node) : root.children) {
                if (!includeAliases && node.redirect != null) continue;

                // Test the permissions on the command
                if (node.condition != null) {
                    var result = node.condition.test(sender, new CommandNode.ConditionContext(sender, CommandContext.Pass.SUGGEST));
                    if (result == CommandCondition.HIDE) continue;
                }

                commands.add(Map.entry(argument.id(), node));
            }
            return commands;
        }

        @Override
        public @NotNull Collection<Map.Entry<Argument<?>, CommandNode>> children(@NotNull CommandNode node, @NotNull CommandSender sender) {
            if (node.children == null) return List.of();
            var commands = new ArrayList<Map.Entry<Argument<?>, CommandNode>>();
            for (CommandNode.ArgumentPair(Argument<?> argument, CommandNode child) : node.children) {

                // Test the permissions on the command
                if (child.condition != null) {
                    var result = child.condition.test(sender, new CommandNode.ConditionContext(sender, CommandContext.Pass.SUGGEST));
                    if (result == CommandCondition.HIDE) continue;
                }

                commands.add(Map.entry(argument, child));
            }
            return commands;
        }
    }
}
