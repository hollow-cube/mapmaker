package net.hollowcube.command;

import net.hollowcube.command.CommandCondition;
import net.hollowcube.command.CommandExecutor;
import net.hollowcube.command.arg.Argument2;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Function;

public class CommandBuilder {

    private final CommandNode node;

    public CommandBuilder() {
        this(new CommandNode());
    }

    private CommandBuilder(@NotNull CommandNode node) {
        this.node = node;
    }


    // Children

    public CommandBuilder child(@NotNull String name, @NotNull Function<CommandBuilder, Object> consumer) {
        var childNode = node.nodeFor(Argument2.Literal(name));
        consumer.apply(new CommandBuilder(childNode));
        return this;
    }

    public CommandBuilder child(@NotNull String name, @NotNull Consumer<CommandBuilder> consumer) {
        var childNode = node.nodeFor(Argument2.Literal(name));
        consumer.accept(new CommandBuilder(childNode));
        return this;
    }

    // Execution

    public @NotNull CommandBuilder executes(CommandExecutor executor, Argument2<?>... args) {
        var executableNode = this.node;
        for (var arg : args) {
            executableNode = executableNode.nodeFor(arg);
        }

        executableNode.setExecutor(executor);
        return this;
    }

    // Condition

    public @NotNull CommandBuilder condition(@NotNull CommandCondition condition) {
        node.setCondition(condition);
        return this;
    }

    public @NotNull CommandNode node() {
        return node;
    }

}
