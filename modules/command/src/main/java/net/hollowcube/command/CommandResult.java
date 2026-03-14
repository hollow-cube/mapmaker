package net.hollowcube.command;

import net.hollowcube.command.arg.Argument;
import org.jetbrains.annotations.Nullable;

public sealed interface CommandResult {

    record Success() implements CommandResult {}

    record Denied(CommandNode node) implements CommandResult {}

    record SyntaxError(int start, @Nullable Argument<?> arg, @Nullable String message) implements CommandResult {}

    record NotFound() implements CommandResult {}

    record ExecutionError(Throwable cause) implements CommandResult {}

    // Factories

    static CommandResult.Success success() {
        return new Success();
    }

    static CommandResult.Denied denied(CommandNode node) {
        return new Denied(node);
    }

    static CommandResult.SyntaxError syntaxError(int start, @Nullable Argument<?> arg) {
        return new SyntaxError(start, arg, null);
    }

    static CommandResult.SyntaxError syntaxError(int start, @Nullable Argument<?> arg, @Nullable String message) {
        return new SyntaxError(start, arg, message);
    }

    static CommandResult.ExecutionError execError(Throwable cause) {
        return new ExecutionError(cause);
    }

    static CommandResult.NotFound notFound() {
        return new NotFound();
    }

}
