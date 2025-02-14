package net.hollowcube.command;

import net.hollowcube.command.arg.Argument;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public sealed interface CommandResult {

    record Success() implements CommandResult {}

    record Denied(@NotNull CommandNode node) implements CommandResult {}

    record SyntaxError(int start, @Nullable Argument<?> arg, boolean isNotFound, @Nullable String message) implements CommandResult {}

    record NotFound() implements CommandResult {}

    record ExecutionError(Throwable cause) implements CommandResult {}

    // Factories

    static @NotNull CommandResult.Success success() {
        return new Success();
    }

    static @NotNull CommandResult.Denied denied(@NotNull CommandNode node) {
        return new Denied(node);
    }

    static @NotNull CommandResult.SyntaxError syntaxError(int start, @Nullable Argument<?> arg) {
        return new SyntaxError(start, arg, false, null);
    }

    static @NotNull CommandResult.SyntaxError syntaxError(int start, @Nullable Argument<?> arg, @Nullable String message) {
        return new SyntaxError(start, arg, false, message);
    }

    static @NotNull CommandResult.ExecutionError execError(@NotNull Throwable cause) {
        return new ExecutionError(cause);
    }

    static @NotNull CommandResult.NotFound notFound() {
        return new NotFound();
    }

}
