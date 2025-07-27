package net.hollowcube.command;

import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.util.CommandCategory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class CommandBuilder {

    private final CommandNode node;

    public CommandBuilder() {
        this(new CommandNode());
    }

    private CommandBuilder(@NotNull CommandNode node) {
        this.node = node;
    }


    // Children

    public CommandBuilder child(@NotNull String name, @NotNull Consumer<CommandBuilder> consumer) {
        var childNode = node.nodeFor(Argument.Literal(name));
        consumer.accept(new CommandBuilder(childNode));
        return this;
    }

    public CommandBuilder child(@NotNull Argument<?> argument, @NotNull Consumer<CommandBuilder> consumer) {
        var childNode = node.nodeFor(argument);
        consumer.accept(new CommandBuilder(childNode));
        return this;
    }

    public CommandBuilder redirect(@NotNull CommandNode target) {
        node.setRedirect(target);
        return this;
    }

    // Execution

    public @NotNull CommandBuilder executes(CommandExecutor executor, Argument<?>... args) {
        var executableNode = this.node;
        for (var arg : args) {
            executableNode = executableNode.nodeFor(arg);
        }

        executableNode.setExecutor(executor);
        return this;
    }

    // Suggestions

    public @NotNull CommandBuilder suggestion(@NotNull CommandExecutor onSuggestion, @NotNull Argument<?>... args) {
        var node = this.node;
        for (var arg : args) {
            node.setOnSuggestion(onSuggestion);
            node = node.nodeFor(arg);
        }

        node.setOnSuggestion(onSuggestion);
        return this;
    }

    // Condition

    public @NotNull CommandBuilder condition(@NotNull CommandCondition condition) {
        node.setCondition(condition);
        return this;
    }

    // Metadata

    public @NotNull CommandBuilder category(@Nullable CommandCategory category) {
        node.category = category;
        return this;
    }

    public @NotNull CommandBuilder description(@Nullable String description) {
        node.description = description;
        return this;
    }

    public @NotNull CommandBuilder examples(@NotNull String... examples) {
        node.examples = List.of(examples);
        return this;
    }

    public @NotNull CommandBuilder examples(@Nullable List<String> examples) {
        node.examples = examples;
        return this;
    }

    public @NotNull CommandNode node() {
        return node;
    }

}
