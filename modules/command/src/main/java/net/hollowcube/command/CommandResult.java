package net.hollowcube.command;

import net.hollowcube.command.arg.Argument;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public sealed interface CommandResult permits CommandResult.Success, CommandResult.SyntaxError, CommandResult.Denied, CommandResult.ExecutionError {

    record Success() implements CommandResult {

    }

    record SyntaxError(int start, @Nullable Argument<?> arg) implements CommandResult {

    }

    record Denied() implements CommandResult {

    }

    record ExecutionError(@NotNull Throwable error) implements CommandResult {

    }

}
