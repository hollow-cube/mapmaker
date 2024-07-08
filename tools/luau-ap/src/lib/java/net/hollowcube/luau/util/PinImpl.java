package net.hollowcube.luau.util;

import net.hollowcube.luau.LuaState;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@ApiStatus.Internal
public final class PinImpl<T> implements Pin<T> {
    private final T value;

    private LuaState state;
    private int ref;

    public PinImpl(@NotNull T value) {
        this.value = value;
    }

    @Override
    public @NotNull T get() {
        return value;
    }

    public void push(@NotNull LuaState state) {
        if (this.state != null && this.state != state) {
            throw new IllegalStateException("Pin is already pinned to another LuaState");
        }
        if (ref == 0) {
            this.state = state;
            state.newUserData(value);
            state.getMetaTable(value.getClass().getName());
            state.setMetaTable(-2);
            ref = state.ref(-1);
            return; // It is still on the stack
        }
        state.getref(ref);
    }

    @Override
    public void close() {
        if (ref == 0) return;

        state.unref(ref);
        state = null;
        ref = 0;

        if (value instanceof Pinned pinned)
            pinned.unpin();
    }
}
