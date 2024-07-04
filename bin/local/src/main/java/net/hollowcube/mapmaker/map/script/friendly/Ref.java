package net.hollowcube.mapmaker.map.script.friendly;

import net.hollowcube.luau.LuaState;
import org.jetbrains.annotations.NotNull;

public class Ref<T extends LuaObject> {
    private final T value;
    private final int ref;

    public Ref(@NotNull LuaState state, @NotNull T value) {
        this.value = value;

        state.newUserData(value);
        state.getMetaTable(value.luaTypeName());
        state.setMetaTable(-2);

        this.ref = state.ref(-1);
        state.pop(1);
    }

    public @NotNull T value() {
        return value;
    }

    public void push(@NotNull LuaState state) {
        state.getref(ref);
    }

    public void close(@NotNull LuaState state) {
        state.unref(ref);
        value.close(state);
    }

}
