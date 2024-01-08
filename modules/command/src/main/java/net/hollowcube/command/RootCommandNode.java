package net.hollowcube.command;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.hollowcube.command.arg.Argument;
import net.minestom.server.entity.Player;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.play.DeclareCommandsPacket;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Locale;

import static net.minestom.server.network.NetworkBuffer.VAR_INT;

final class RootCommandNode extends CommandNode {

    @Override
    @NotNull
    CommandNode nodeFor(@NotNull Argument<?> argument) {
        throw new UnsupportedOperationException("use register() to manage root node children");
    }

    void register(@NotNull String name, @NotNull CommandNode node) {
        if (this.children == null) this.children = new ArrayList<>();

        for (ArgumentPair(Argument<?> argument, CommandNode unused) : this.children) {
            if (argument.id().equalsIgnoreCase(name)) {
                throw new IllegalArgumentException("duplicate child name: " + name);
            }
        }

        this.children.add(new ArgumentPair(Argument.Literal(name), node));
    }

    public @NotNull DeclareCommandsPacket createCommandPacket(@NotNull Player player) {
        var nodes = new ArrayList<DeclareCommandsPacket.Node>();
        var root = new DeclareCommandsPacket.Node();
        nodes.add(root);

        var rootNodes = new IntArrayList();
        for (ArgumentPair(Argument<?> argument, CommandNode childNode) : children) {
            if (childNode.condition != null) {
                var result = childNode.condition.test(player, new ConditionContext(player, CommandContext.Pass.BUILD));
                if (result == CommandCondition.HIDE) continue;
            }

            var args = new DeclareCommandsPacket.Node();
            args.flags = DeclareCommandsPacket.getFlag(DeclareCommandsPacket.NodeType.ARGUMENT, true, false, true);
            args.name = "args";
            args.parser = "brigadier:string";
            args.properties = NetworkBuffer.makeArray(buffer -> buffer.write(VAR_INT, 2));
            args.suggestionsType = "minecraft:ask_server";
            nodes.add(args);

            var node = new DeclareCommandsPacket.Node();
            node.flags = DeclareCommandsPacket.getFlag(DeclareCommandsPacket.NodeType.LITERAL, true, false, false);
            node.name = argument.id().toLowerCase(Locale.ROOT);
            node.children = new int[]{nodes.size() - 1};
            rootNodes.add(nodes.size());
            nodes.add(node);
        }
        root.children = rootNodes.toIntArray();

        return new DeclareCommandsPacket(nodes, 0);
    }
}
