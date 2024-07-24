package net.hollowcube.luau.error;

import org.jetbrains.annotations.NotNull;

public class LuaArgError extends RuntimeException {
    private final int index;
    private final String message;

    /**
     * @param index   The problematic argument, indexed from 0.
     * @param message
     */
    public LuaArgError(int index, @NotNull String message) {
        this.index = index;
        this.message = message;
    }
}
