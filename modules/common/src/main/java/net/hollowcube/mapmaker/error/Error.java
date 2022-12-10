package net.hollowcube.mapmaker.error;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;

public class Error extends RuntimeException {

    public static @NotNull Error of(@NotNull String message) {
        return new Error(message, null);
    }

    public Error(@NotNull String message, @Nullable Throwable cause) {
        super(message, cause);
    }

    public @NotNull Error wrap(@NotNull String message) {
        return new Error(MessageFormat.format(message, this.getMessage()), this);
    }

    public boolean is(@NotNull Error error) {
        if (this == error) return true;
        if (getCause() instanceof Error cause)
            return cause.is(error);
        return false;
    }

}
