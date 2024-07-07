package net.hollowcube.luau.util;

import org.jetbrains.annotations.NotNull;

public sealed interface Pin<T> permits PinImpl {

    static <T> @NotNull Pin<T> value(@NotNull T value) {
        return new PinImpl<>(value);
    }

    void close();
}
