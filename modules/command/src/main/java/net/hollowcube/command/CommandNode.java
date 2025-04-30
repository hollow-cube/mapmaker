package net.hollowcube.command;

import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ParseResult;
import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.suggestion.SuggestionContext;
import net.hollowcube.command.util.CommandCategory;
import net.hollowcube.command.util.StringReader;
import net.minestom.server.command.CommandSender;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A command node represents a single node of a command syntax tree.
 *
 * <p>It may be executable, and it may have child nodes which are again just command nodes.
 * It also may have an associated argument which must parse correctly to enter that node.</p>
 */
public class CommandNode {

    protected CommandNode redirect = null; // May not be used with executor, children, condition.
    protected boolean shouldSuggest = true;

    protected CommandExecutor executor = null; // May not be used with redirect
    protected CommandExecutor onSuggestion = null; // May not be used with redirect
    protected List<ArgumentPair> children = null; // May not be used with redirect
    protected CommandCondition condition = null; // May not be used with redirect

    // Metadata
    protected CommandCategory category = CommandCategory.DEFAULT;
    protected String description = null;
    protected List<String> examples = null;

    public CommandNode() {

    }

    public @Nullable CommandNode redirect() {
        return redirect;
    }

    public @Nullable CommandCategory category() {
        return category;
    }

    public @Nullable String description() {
        return description;
    }

    public @Nullable List<String> examples() {
        return examples;
    }

    public boolean shouldSuggest() {
        return shouldSuggest;
    }

    public void cancelSuggestions() {
        shouldSuggest = false;
    }

    @Contract(pure = true)
    public @Nullable List<ArgumentPair> children() {
        return children;
    }

    public @Nullable CommandCondition condition() {
        return condition;
    }

    public @UnknownNullability CommandNode xpath(@NotNull String path, boolean followRedirects) {
        // If its a redirect always defer to the directed node.
        if (followRedirects && redirect != null) {
            return redirect.xpath(path, true);
        }

        // Found this node
        if (path.isEmpty()) return this;

        String next, remaining;
        if (path.indexOf('.') != -1) {
            // If there is a dot, split on it and try to find a child with the first part
            next = path.substring(0, path.indexOf('.'));
            remaining = path.substring(path.indexOf('.') + 1);
        } else {
            // Otherwise the entire path is the first part
            next = path;
            remaining = "";
        }

        for (var pair : children) {
            if (!pair.argument().id().equalsIgnoreCase(next)) continue;

            return pair.node().xpath(remaining, followRedirects);
        }

        return null;
    }

    public @NotNull Suggestion suggest(@NotNull CommandSender sender, @NotNull StringReader reader, @NotNull SuggestionContext context) {
        // If this is a redirect, completely defer handling
        if (redirect != null) {
            if (redirect.condition != null) {
                var result = redirect.condition.test(sender, new ConditionContext(sender, CommandContext.Pass.EXECUTE));
                if (result != CommandCondition.ALLOW) return Suggestion.EMPTY;
            }

            return redirect.suggest(sender, reader, context);
        }

        // It is worth noting that we do not execute the condition of this command right here. During suggestion,
        // conditions are executed by the parent node entering them only.

        // If there are no children, or we are at end of input then there cannot be any suggestions
        if (children == null || children.isEmpty() || !reader.canRead()) return Suggestion.EMPTY;

        var mark = reader.mark();
        // the +1 here is to skip the space, but i think its kinda wrong
        var suggestion = new Suggestion(reader.pos(mark) + 1, reader.remaining());
//        var suggestion = new Suggestion(reader.pos(mark) + 1, reader.remaining());
        for (var pair : children) {
            var node = pair.node();
            // If there is a condition on the child node we need to evaluate it
            if (node.condition != null) {
                var result = node.condition.test(sender, new ConditionContext(sender, CommandContext.Pass.SUGGEST));
                if (result == CommandCondition.HIDE) continue;
            }

            var result = pair.argument().parse(sender, reader);

            // If we have more space to read and get an exact match we can suggest the child
            if (result instanceof  ParseResult.Success<?>(var valueFunc)) {
                context.setArgValue(pair.argument.id(), reader.rawSince(mark).trim(), valueFunc.get());
                if (node.onSuggestion != null) {
                    node.onSuggestion.execute(sender, context);
                }
            }
            if (reader.canRead() && result instanceof ParseResult.Success<?>) {
                return node.suggest(sender, reader, context);
            }
            if (result instanceof ParseResult.Partial<?>(var _, var valueFunc) && valueFunc != null) {
                context.setArgValue(pair.argument.id(), reader.rawSince(mark).trim(), valueFunc.get());
                if (node.onSuggestion != null) {
                    node.onSuggestion.execute(sender, context);
                }
            }

            // If we consumed the entire remainder and have a partial match we can suggest the child.
            if (!reader.canRead() && !(result instanceof ParseResult.Failure<?>)) {
                pair.argument().suggest(sender, reader.rawSince(mark), suggestion);
                // Do not return here, we will try to collect suggestions from the other args as well.
            }

            // Otherwise this is a fail and we can try the next syntax
            reader.restore(mark);
        }

        return suggestion;
    }

    public @NotNull CommandResult execute(@NotNull CommandSender sender, @NotNull StringReader reader, @NotNull CommandContextImpl context) {
        // If this is a redirect, completely defer handling
        if (redirect != null) {
            if (redirect.condition != null) {
                var result = redirect.condition.test(sender, new ConditionContext(sender, CommandContext.Pass.EXECUTE));
                if (result == CommandCondition.DENY) return CommandResult.denied(this);
                if (result == CommandCondition.HIDE) return CommandResult.notFound();
            }

            return redirect.execute(sender, reader, context);
        }

        if (reader.remaining() == 0) {
            // No more to read, and we have an executor, run it.
            if (executor != null) {
                try {
                    executor.execute(sender, context);
                    return CommandResult.success();
                } catch (Exception e) {
                    return CommandResult.execError(e);
                }
            }

            // Otherwise we reached end of input without an executable command (i think), so its an error.
            return CommandResult.syntaxError(reader.pos(), null);
        }

        // Try to apply one of the children
        if (children != null && !children.isEmpty()) {
            CommandResult pendingError = null;
            for (var pair : children) {
                var node = pair.node();
                var argument = pair.argument();
                // If there is a condition on the child we need to evaluate it
                if (node.condition != null) {
                    var result = node.condition.test(sender, new ConditionContext(sender, CommandContext.Pass.EXECUTE));
                    if (result == CommandCondition.DENY) {
                        if (pendingError == null) pendingError = CommandResult.denied(this);
                        continue;
                    }
                    if (result == CommandCondition.HIDE) {
                        continue;
                    }
                }

                // Try to parse the childs argument
                var mark = reader.mark();
                var result = argument.parse(sender, reader);
                if (result instanceof ParseResult.Success<?>(var valueFunc)) {
                    // If it succeeds we have matched and can execute the child
                    context.setArgValue(argument.id(), reader.rawSince(mark).trim(), valueFunc.get());
                    return node.execute(sender, reader, context);
                }

                // Otherwise store the error and try the next argument.
                if (pendingError == null) {
                    if (result instanceof ParseResult.Failure<?>(var start, var message)) {
                        var errorStart = start == -1 ? reader.pos(mark) : start;
                        pendingError = CommandResult.syntaxError(errorStart, argument, message);
                    } else if (result instanceof ParseResult.Partial<?>(var message, var _)) {
                        pendingError = CommandResult.syntaxError(reader.pos(), argument, message);
                    } else {
                        pendingError = CommandResult.syntaxError(reader.pos(), argument);
                    }
                }

                reader.restore(mark);
            }

            // If we reach here, we have no matching child, so return the pending error
            if (pendingError != null) {
                if (pendingError instanceof CommandResult.SyntaxError && this instanceof RootCommandNode) {
                    return CommandResult.notFound();
                } else {
                    return pendingError;
                }
            }
        }

        // At this point we reached a leaf node but we still have input left so its an error
        return CommandResult.notFound();
    }


    // Introspection

    public boolean isExecutable() {
        return this.executor != null;
    }

    public boolean isConditional() {
        return this.condition != null;
    }


    // Modification

    void setRedirect(@NotNull CommandNode node) {
        Check.stateCondition(this.executor != null, "Cannot add redirect to an executable node!");
        Check.stateCondition(this.children != null, "Cannot add redirect to a node with children!");
        Check.stateCondition(this.condition != null, "Cannot add redirect to a conditional node!");
        this.redirect = Objects.requireNonNull(node);
    }

    void setExecutor(@NotNull CommandExecutor executor) {
        Check.stateCondition(this.redirect != null, "Cannot add an executor to a redirect node!");
        Check.stateCondition(this.executor != null, "Command node already has an executor!");
        this.executor = Objects.requireNonNull(executor);
    }

    void setCondition(@NotNull CommandCondition condition) {
        Check.stateCondition(this.redirect != null, "Cannot add condition to a redirect node!");
        Check.stateCondition(this.condition != null, "Command node already has a condition!");
        this.condition = Objects.requireNonNull(condition);
    }

    void setOnSuggestion(@NotNull CommandExecutor onSuggestion) {
        this.onSuggestion = onSuggestion;
    }

    /**
     * Returns the child node for the given argument, or adds one if it doesn't already exist.
     *
     * <p>Arguments are matched by id, and must be equal if the argument already exists.</p>
     */
    @NotNull
    CommandNode nodeFor(@NotNull Argument<?> argument) {
        Check.stateCondition(this.redirect != null, "Cannot add child to a redirect node!");
        if (children == null) children = new ArrayList<>();
        for (var pair : children) {
            var existing = pair.argument();
            if (!existing.id().equalsIgnoreCase(argument.id())) continue;

            // Test equality on the argument objects
            Check.argCondition(!existing.equals(argument), "Argument '" + argument.id() + "' already exists but differs in value: " + existing + " != " + argument);

            return pair.node();
        }

        // Argument was not found so add it
        var node = new CommandNode();
        children.add(new ArgumentPair(argument, node));
        return node;
    }

    public void visitChildren(@NotNull Consumer<@NotNull ArgumentPair> visitor) {
        if (this.children != null) {
            this.children.forEach(argumentPair -> {
                visitor.accept(argumentPair);
                argumentPair.node.visitChildren(visitor);
            });
        }
    }

    public record ArgumentPair(@NotNull Argument<?> argument, @NotNull CommandNode node) {
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
