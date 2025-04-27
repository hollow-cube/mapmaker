package net.hollowcube.command.builder;

import net.hollowcube.command.CommandNode;
import net.minestom.server.command.ArgumentParserType;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.play.DeclareCommandsPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CommandNodeBuilder {
    public byte flags;
    public List<CommandNode> children = new ArrayList<>();
    public CommandNode redirectedNode; // Only if flags & 0x08
    public String name = ""; // Only for literal and argument
    public ArgumentParserType parser; // Only for argument
    public byte[] properties; // Only for argument
    public String suggestionsType = ""; // Only if flags 0x10

    public CommandNodeBuilder(String name, CommandNode node) {
        this.name = name;
        this.flags = DeclareCommandsPacket.getFlag(DeclareCommandsPacket.NodeType.LITERAL, node.isExecutable(), node.redirect() != null, false);
        if (node.redirect() != null) {
            this.redirectedNode = node.redirect();
        }
        if (node.children() != null) {
            this.children.addAll(node.children().stream().map(CommandNode.ArgumentPair::node).toList());
        }
    }

    public CommandNodeBuilder(CommandNode.ArgumentPair argumentPair) {
        var argument = argumentPair.argument();
        var node = argumentPair.node();
        this.flags = DeclareCommandsPacket.getFlag(argumentPair.argument().getType(), node.isExecutable(), node.redirect() != null, argumentPair.argument().getType() == DeclareCommandsPacket.NodeType.ARGUMENT);

        this.name = argument.id();

        if (node.redirect() != null) {
            this.redirectedNode = node.redirect();
            return;
        }

        if (argument.getType() == DeclareCommandsPacket.NodeType.ARGUMENT) {
            this.suggestionsType = "minecraft:ask_server";
            this.parser = argument.argumentType();
            this.properties = NetworkBuffer.makeArray(argument::properties);
        }
        var children = node.children();
        if (children != null) {
            this.children.addAll(children.stream().map(CommandNode.ArgumentPair::node).toList());
        }
    }

    public DeclareCommandsPacket.Node toNode(CommandEvaluationContext context) {
        var packetNode = new DeclareCommandsPacket.Node();

        packetNode.flags = this.flags;
        packetNode.name = this.name;
        packetNode.parser = this.parser;
        packetNode.properties = this.properties;
        packetNode.suggestionsType = this.suggestionsType;
        if (this.redirectedNode != null) {
            var redirect = context.getId(this.redirectedNode);
            if (redirect != null)
                packetNode.redirectedNode = redirect;
        }
        packetNode.children = this.children.stream()
                .map(context::getId)
                .filter(Objects::nonNull)
                .mapToInt(i -> i).toArray();

        return packetNode;
    }
}
