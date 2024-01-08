package net.hollowcube.command;

import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.util.StringReader;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.DeclareCommandsPacket;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class CommandManagerImpl implements CommandManager {
    private final RootCommandNode root = new RootCommandNode();

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
        register(command.name(), command::build);
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
    public @NotNull DeclareCommandsPacket createCommandPacket(@NotNull Player player) {
        return root.createCommandPacket(player);
    }
}
