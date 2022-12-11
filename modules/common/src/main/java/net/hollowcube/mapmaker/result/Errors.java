package net.hollowcube.mapmaker.result;

import org.jetbrains.annotations.NotNull;

class Errors {

    public static boolean is(@NotNull Error error, @NotNull Error other) {
        if (error == other) return true;
        if (error instanceof WrappedError wrapped)
            return is(wrapped.error, other);
        if (error instanceof MultiError multi) {
            for (Error e : multi.errors)
                if (is(e, other))
                    return true;
        }
        return false;
    }

    public record SimpleError(@NotNull String message) implements Error {
        @Override
        public String toString() {
            return message;
        }
    }

    public record WrappedError(@NotNull String message, @NotNull Error error) implements Error {
        @Override
        public String toString() {
            return message;
        }
    }

    public record MultiError(@NotNull Error[] errors) implements Error {

        @Override
        public @NotNull String message() {
            var sb = new StringBuilder("multiple errors: ");
            for (int i = 0; i < errors.length; i++) {
                sb.append(errors[i]);
                if (i < errors.length - 1)
                    sb.append(", ");
            }
            return sb.toString();
        }

        @Override
        public String toString() {
            return message();
        }
    }

    public static final class ExceptionError implements Error {
        private final Throwable exception;

        public ExceptionError(@NotNull Throwable exception) {
            this.exception = exception;
        }

        @Override
        public @NotNull String message() {
            //todo probably want to include the stacktrace or something
            return exception.getMessage();
        }

        @Override
        public String toString() {
            return message();
        }
    }
}
