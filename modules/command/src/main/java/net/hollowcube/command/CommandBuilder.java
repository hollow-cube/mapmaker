package net.hollowcube.command;

import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.util.CommandCategory;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public final class CommandBuilder {

    private final CommandNode node;

    public CommandBuilder() {
        this(new CommandNode());
    }

    private CommandBuilder(CommandNode node) {
        this.node = node;
    }

    // Children

    public CommandBuilder child(String name, Consumer<CommandBuilder> consumer) {
        var childNode = node.nodeFor(Argument.Literal(name));
        consumer.accept(new CommandBuilder(childNode));
        return this;
    }

    public CommandBuilder child(Argument<?> argument, Consumer<CommandBuilder> consumer) {
        var childNode = node.nodeFor(argument);
        consumer.accept(new CommandBuilder(childNode));
        return this;
    }

    public CommandBuilder redirect(CommandNode target) {
        node.setRedirect(target);
        return this;
    }

    // Execution

    public CommandBuilder executes(CommandExecutor executor, Argument<?>... args) {
        var executableNode = this.node;
        for (var arg : args) {
            executableNode = executableNode.nodeFor(arg);
        }

        executableNode.setExecutor(executor);
        return this;
    }

    // Suggestions

    public CommandBuilder suggestion(CommandExecutor onSuggestion, Argument<?>... args) {
        var node = this.node;
        for (var arg : args) {
            node.setOnSuggestion(onSuggestion);
            node = node.nodeFor(arg);
        }

        node.setOnSuggestion(onSuggestion);
        return this;
    }

    // Condition

    public CommandBuilder condition(CommandCondition condition) {
        node.setCondition(condition);
        return this;
    }

    // Metadata

    public CommandBuilder category(@Nullable CommandCategory category) {
        node.category = category;
        return this;
    }

    public CommandBuilder description(@Nullable String description) {
        node.description = description;
        return this;
    }

    public CommandBuilder examples(String... examples) {
        node.examples = List.of(examples);
        return this;
    }

    public CommandBuilder examples(@Nullable List<String> examples) {
        node.examples = examples;
        return this;
    }

    public CommandNode node() {
        return node;
    }

}
