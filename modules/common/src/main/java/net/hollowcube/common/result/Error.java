package net.hollowcube.common.result;

import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;

@SuppressWarnings("JavaLangClash") //todo probably should rename to Err or something
public sealed interface Error permits Errors.ExceptionError, Errors.MultiError, Errors.SimpleError, Errors.WrappedError {

    static @NotNull Error of(@NotNull String message) {
        return new Errors.SimpleError(message);
    }

    static @NotNull Error of(@NotNull Throwable exception) {
        return new Errors.ExceptionError(exception);
    }

    @NotNull String message();

    default @NotNull Error wrap(@NotNull String message) {
        return new Errors.WrappedError(MessageFormat.format(message, this.message()), this);
    }

    default boolean is(@NotNull Error error) {
        return Errors.is(this, error);
    }

}
