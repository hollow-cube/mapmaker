package net.hollowcube.common.result;

import org.jetbrains.annotations.NotNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MultiError that)) return false;
            return Arrays.equals(errors, that.errors);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(errors);
        }
    }

    public static final class ExceptionError implements Error {
        private static final boolean VERBOSE_EXCEPTIONS = Integer.getInteger("hollowcube.verbose_exceptions", 1) != 0;

        private final Throwable exception;

        public ExceptionError(@NotNull Throwable exception) {
            this.exception = exception;
        }

        @Override
        public @NotNull String message() {
            if (VERBOSE_EXCEPTIONS) {
                var writer = new StringWriter();
                exception.printStackTrace(new PrintWriter(writer));
                return writer.toString();
            }
            //todo probably want to include the stacktrace or something
            return exception.getMessage();
        }

        @Override
        public String toString() {
            return message();
        }
    }
}
