package net.hollowcube.mapmaker.result;

import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;

public sealed interface Error permits Errors.MultiError, Errors.SimpleError, Errors.WrappedError {

    static @NotNull Error of(@NotNull String message) {
        return new Errors.SimpleError(message);
    }

    @NotNull String message();

    default @NotNull Error wrap(@NotNull String message) {
        return new Errors.WrappedError(MessageFormat.format(message, this.message()), this);
    }

}
