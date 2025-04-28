package net.hollowcube.command.builder;

import net.hollowcube.command.CommandNode;
import net.minestom.server.command.ArgumentParserType;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.play.DeclareCommandsPacket;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class CommandNodeBuilder {
    private static final Logger log = LoggerFactory.getLogger(CommandNodeBuilder.class);
    public byte flags;
    public List<CommandNode> children = new ArrayList<>();
    public CommandNode redirectedNode; // Only if flags & 0x08
    public String name; // Only for literal and argument
    public ArgumentParserType parser; // Only for argument
    public byte[] properties; // Only for argument
    public String suggestionsType = ""; // Only if flags 0x10

    public CommandNodeBuilder(@NotNull String name, @NotNull CommandNode node) {
        this.name = name;
        this.flags = DeclareCommandsPacket.getFlag(DeclareCommandsPacket.NodeType.LITERAL, node.isExecutable(), node.redirect() != null, false);
        if (node.redirect() != null) {
            this.redirectedNode = node.redirect();
        }
        if (node.children() != null) {
            ensureOneSuggestion(node.children(), name);
            this.children.addAll(node.children().stream().map(CommandNode.ArgumentPair::node).toList());
        }
    }

    public CommandNodeBuilder(@NotNull CommandNode.ArgumentPair argumentPair) {
        var argument = argumentPair.argument();
        var node = argumentPair.node();
        this.flags = DeclareCommandsPacket.getFlag(argumentPair.argument().getType(), node.isExecutable(), node.redirect() != null, argumentPair.argument().getType() == DeclareCommandsPacket.NodeType.ARGUMENT && node.shouldSuggest() && argumentPair.argument().shouldSuggest());

        this.name = argument.id();

        if (node.redirect() != null) {
            this.redirectedNode = node.redirect();
            return;
        }

        if (argument.getType() == DeclareCommandsPacket.NodeType.ARGUMENT) {
            if (node.shouldSuggest()) {
                this.suggestionsType = "minecraft:ask_server";
            }
            this.parser = argument.argumentType();
            this.properties = NetworkBuffer.makeArray(argument::properties);
        }
        var children = node.children();
        if (children != null) {
            ensureOneSuggestion(children, argument.id());
            this.children.addAll(children.stream().map(CommandNode.ArgumentPair::node).toList());
        }
    }

    private static void ensureOneSuggestion(@NotNull List<CommandNode.ArgumentPair> children, @NotNull  String name) {
        var suggestingArgumentChildren = children.stream().filter(node -> node.argument().getType() == DeclareCommandsPacket.NodeType.ARGUMENT && node.argument().shouldSuggest()).toList();
        final long count = suggestingArgumentChildren.size();
        if (count > 1) {
            log.debug("More then two argument children for node {}: [{}]", name, suggestingArgumentChildren.stream().map(pair -> pair.argument().id()).collect(Collectors.joining(", ")));
            suggestingArgumentChildren.stream()
                    // always gives the suggestion to a score holder if one is present
                    .sorted(Comparator.<CommandNode.ArgumentPair>comparingInt(pair -> priority(pair.argument().argumentType())).reversed())
                    .skip(1).forEach(pair -> pair.node().cancelSuggestions());
        }
    }

    private static int priority(@NotNull ArgumentParserType type) {
        if (type == ArgumentParserType.SCORE_HOLDER) {
            return 10;
        }
        return 0;
    }

    public @NotNull  DeclareCommandsPacket.Node toNode(@NotNull CommandEvaluationContext context) {
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
