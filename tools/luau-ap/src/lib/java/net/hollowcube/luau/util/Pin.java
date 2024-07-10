package net.hollowcube.luau.util;

import net.hollowcube.luau.LuaState;
import org.jetbrains.annotations.NotNull;

public sealed interface Pin<T> permits PinImpl {

    static <T> @NotNull Pin<T> value(@NotNull T value) {
        return new PinImpl<>(value);
    }

    /**
     * Create a pin for the given ref with the given value. The ref is now owned by the pin, you should not
     * manipulate it further.
     *
     * <p>This method is inherently unsafe, the refs are not validated.</p>
     */
    static <T> @NotNull Pin<T> fromRef(@NotNull LuaState state, int ref, @NotNull T value) {
        return new PinImpl<>(state, ref, value);
    }

    @NotNull T get();

    void close();
}
