package net.hollowcube.command;

import net.hollowcube.command.arg.Argument;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public sealed interface CommandResult permits CommandResult.Denied, CommandResult.ExecutionError,
        CommandResult.Success, CommandResult.SyntaxError {

    // Factories

    static @NotNull CommandResult.Success success() {
        return new CommandResultImpl.SuccessImpl();
    }

    static @NotNull CommandResult.Denied denied(@NotNull CommandNode node) {
        return new CommandResultImpl.DeniedImpl(node);
    }

    static @NotNull CommandResult.SyntaxError syntaxError(int start, @Nullable Argument<?> arg) {
        return new CommandResultImpl.SyntaxErrorImpl(start, arg, false);
    }

    static @NotNull CommandResult.SyntaxError syntaxError(int start, @Nullable Argument<?> arg, boolean isNotFound) {
        return new CommandResultImpl.SyntaxErrorImpl(start, arg, isNotFound);
    }

    static @NotNull CommandResult.ExecutionError execError(@NotNull Throwable cause) {
        return new CommandResultImpl.ExecutionErrorImpl(cause);
    }


    // Impl

    boolean isSuccess();


    // Types

    sealed interface Success extends CommandResult permits CommandResultImpl.SuccessImpl {
    }

    /**
     * Occurs when a {@link CommandCondition} returns a {@link CommandCondition#DENY} result.
     */
    sealed interface Denied extends CommandResult permits CommandResultImpl.DeniedImpl {
        @NotNull CommandNode node();
    }

    sealed interface SyntaxError extends CommandResult permits CommandResultImpl.SyntaxErrorImpl {
        int start();

        @Nullable Argument<?> arg();

        /**
         * Occurs when a root command node is not found. It will never happen beyond the root node match.
         */
        boolean isNotFound();
        //todo need to revisit the implementation of notfound, i am not very happy with it.
    }

    /**
     * Occurs when an exception is thrown in any argument, condition, or executor (generally "user code").
     */
    sealed interface ExecutionError extends CommandResult permits CommandResultImpl.ExecutionErrorImpl {
        @NotNull Throwable cause();
    }

}
