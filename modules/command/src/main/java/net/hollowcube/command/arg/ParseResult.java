package net.hollowcube.command.arg;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.function.Supplier;

public sealed interface ParseResult<T extends @UnknownNullability Object> permits ParseResult.Partial, ParseResult.Success, ParseResult.Failure {

    record Partial<T extends @UnknownNullability Object>(
        @Nullable String message,
        @Nullable Supplier<@UnknownNullability T> valueFunc
    ) implements ParseResult<T> {

        public Partial(@Nullable String message) {
            this(message, null);
        }

        public Partial() {
            this(null);
        }
    }

    record Success<T extends @UnknownNullability Object>(
        Supplier<@UnknownNullability T> valueFunc
    ) implements ParseResult<T> {

        public Success(@UnknownNullability T value) {
            this(() -> value);
        }

    }

    record Failure<T extends @Nullable Object>(
        int start,
        @Nullable String message
    ) implements ParseResult<T> {

        public Failure(int start) {
            this(start, null);
        }
    }

}
