package net.hollowcube.command;

import net.hollowcube.command.arg.Argument2;
import org.jetbrains.annotations.Nullable;

public sealed interface CommandResult permits CommandResult.Success, CommandResult.SyntaxError, CommandResult.Denied {

    record Success() implements CommandResult {

    }

    record SyntaxError(int start, @Nullable Argument2<?> arg) implements CommandResult {

    }

    record Denied() implements CommandResult {

    }

}
