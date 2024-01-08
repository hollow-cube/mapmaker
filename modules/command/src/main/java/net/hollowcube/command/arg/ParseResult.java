package net.hollowcube.command.arg;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.util.function.Supplier;

public sealed interface ParseResult<T> permits ParseResult.Partial, ParseResult.Success, ParseResult.Failure {

    record Partial<T>() implements ParseResult<T> {

    }

    record Success<T>(@NotNull Supplier<@UnknownNullability T> valueFunc) implements ParseResult<T> {

        public Success(@NotNull T value) {
            this(() -> value);
        }

    }

    record Failure<T>(int start) implements ParseResult<T> {

    }

}
