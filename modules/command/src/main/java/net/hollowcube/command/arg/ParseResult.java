package net.hollowcube.command.arg;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.function.Supplier;

public sealed interface ParseResult<T> permits ParseResult.Partial, ParseResult.Success, ParseResult.Failure {

    record Partial<T>(@Nullable String message) implements ParseResult<T> {

        public Partial() {
            this(null);
        }
    }

    record Success<T>(@NotNull Supplier<@UnknownNullability T> valueFunc) implements ParseResult<T> {

        public Success(@NotNull T value) {
            this(() -> value);
        }

    }

    record Failure<T>(int start, @Nullable String message) implements ParseResult<T> {

        public Failure(int start) {
            this(start, null);
        }
    }

}
