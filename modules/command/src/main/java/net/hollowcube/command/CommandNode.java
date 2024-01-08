package net.hollowcube.command;

import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ParseResult;
import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.util.StringReader;
import net.minestom.server.command.CommandSender;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A command node represents a single node of a command syntax tree.
 *
 * <p>It may be executable, and it may have child nodes which are again just command nodes.
 * It also may have an associated argument which must parse correctly to enter that node.</p>
 */
public class CommandNode {

    private CommandExecutor executor = null;
    protected List<ArgumentPair> children = null;

    protected CommandCondition condition = null;

    public CommandNode() {

    }

    public @NotNull Suggestion suggest(@NotNull CommandSender sender, @NotNull StringReader reader) {
        // If there are no children, or we are at end of input then there cannot be any suggestions
        if (children == null || children.isEmpty() || !reader.canRead()) return Suggestion.EMPTY;

        // If there is a condition on this node we need to evaluate it
        if (condition != null) {
            var result = condition.test(sender, new ConditionContext(sender, CommandContext.Pass.SUGGEST));
            if (result == CommandCondition.HIDE) return Suggestion.EMPTY;
        }

        var mark = reader.mark();
        // the +1 here is to skip the space, but i think its kinda wrong
        var suggestion = new Suggestion(reader.pos(mark) + 1, reader.remaining());
        for (ArgumentPair(Argument<?> argument, CommandNode node) : children) {
            var result = argument.parse(sender, reader);

            // If we have more space to read and get an exact match we can suggest the child
            if (reader.canRead() && result instanceof ParseResult.Success<?>) {
                return node.suggest(sender, reader);
            }

            // If we consumed the entire remainder and have a partial match we can suggest the child.
            if (!reader.canRead() && !(result instanceof ParseResult.Failure<?>)) {
                argument.suggest(sender, reader.rawSince(mark), suggestion);
                // Do not return here, we will try to collect suggestions from the other args as well.
            }

            // Otherwise this is a fail and we can try the next syntax
            reader.restore(mark);
        }

        return suggestion;
    }

    public @NotNull CommandResult execute(@NotNull CommandSender sender, @NotNull StringReader reader, @NotNull CommandContextImpl context) {
        // If there is a condition on this node we need to evaluate it
        if (condition != null) {
            var result = condition.test(sender, new ConditionContext(sender, CommandContext.Pass.EXECUTE));
            if (result == CommandCondition.DENY) return new CommandResult.Denied();
            if (result == CommandCondition.HIDE) return new CommandResult.SyntaxError(reader.pos(), null);
        }

        if (reader.remaining() == 0) {
            // No more to read, and we have an executor, run it.
            if (executor != null) {
                executor.execute(sender, context);
                return new CommandResult.Success();
            }

            // Otherwise we reached end of input without an executable command (i think), so its an error.
            return new CommandResult.SyntaxError(reader.pos(), null);
        }

        // Try to apply one of the children
        if (children != null && !children.isEmpty()) {
            CommandResult pendingError = null;
            for (ArgumentPair(Argument<?> argument, CommandNode node) : children) {

                // Try to parse the childs argument
                var mark = reader.mark();
                var result = argument.parse(sender, reader);
                if (result instanceof ParseResult.Success<?> success) {
                    // If it succeeds we have matched and can execute the child
                    context.setArgValue(argument.id(), reader.rawSince(mark).trim(), success.valueFunc().get());
                    return node.execute(sender, reader, context);
                }

                // Otherwise store the error and try the next argument.
                if (pendingError == null) {
                    if (result instanceof ParseResult.Failure<?> failure) {
                        int errorStart = failure.start() == -1 ? reader.pos(mark) : failure.start();
                        pendingError = new CommandResult.SyntaxError(errorStart, argument);
                    } else {
                        pendingError = new CommandResult.SyntaxError(reader.pos(), argument);
                    }
                }
                reader.restore(mark);
            }

            // If we reach here, we have no matching child, so return the pending error
            return Objects.requireNonNull(pendingError, "pendingError sanity check");
        }

        // At this point we reached a leaf node but we still have input left so its an error
        return new CommandResult.SyntaxError(reader.pos(), null);
    }


    // Introspection

    public boolean isExecutable() {
        return this.executor != null;
    }

    public boolean isConditional() {
        return this.condition != null;
    }


    // Modification

    void setExecutor(@NotNull CommandExecutor executor) {
        Check.stateCondition(this.executor != null, "Command node already has an executor!");
        this.executor = executor;
    }

    void setCondition(@NotNull CommandCondition condition) {
        Check.stateCondition(this.condition != null, "Command node already has a condition!");
        this.condition = condition;
    }

    /**
     * Returns the child node for the given argument, or adds one if it doesn't already exist.
     *
     * <p>Arguments are matched by id, and must be equal if the argument already exists.</p>
     */
    @NotNull
    CommandNode nodeFor(@NotNull Argument<?> argument) {
        if (children == null) children = new ArrayList<>();
        for (ArgumentPair(Argument<?> existing, CommandNode node) : children) {
            if (!existing.id().equalsIgnoreCase(argument.id())) continue;

            // Test equality on the argument objects
            Check.argCondition(!existing.equals(argument), "Argument '" + argument.id() + "' already exists but differs in value: " + existing + " != " + argument);

            return node;
        }

        // Argument was not found so add it
        var node = new CommandNode();
        children.add(new ArgumentPair(argument, node));
        return node;
    }

    protected record ArgumentPair(Argument<?> argument, CommandNode node) {
    }

    protected record ConditionContext(@NotNull CommandSender sender, @NotNull Pass pass) implements CommandContext {
        @Override
        public @UnknownNullability String getRaw(@NotNull Argument<?> arg) {
            return null;
        }

        @Override
        public <T> @UnknownNullability T get(@NotNull Argument<T> arg) {
            return null;
        }

        @Override
        public boolean has(@NotNull Argument<?> arg) {
            return false;
        }
    }

}
