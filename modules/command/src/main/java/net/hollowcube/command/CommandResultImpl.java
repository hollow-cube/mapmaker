package net.hollowcube.command;

import net.hollowcube.command.arg.Argument;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class CommandResultImpl {

    record SuccessImpl() implements CommandResult.Success {
        @Override
        public boolean isSuccess() {
            return true;
        }
    }

    record DeniedImpl(@NotNull CommandNode node) implements CommandResult.Denied {
        @Override
        public boolean isSuccess() {
            return false;
        }
    }

    record SyntaxErrorImpl(int start, @Nullable Argument<?> arg,
                           boolean isNotFound) implements CommandResult.SyntaxError {
        @Override
        public boolean isSuccess() {
            return false;
        }
    }

    record ExecutionErrorImpl(@NotNull Throwable cause) implements CommandResult.ExecutionError {
        @Override
        public boolean isSuccess() {
            return false;
        }
    }
}
