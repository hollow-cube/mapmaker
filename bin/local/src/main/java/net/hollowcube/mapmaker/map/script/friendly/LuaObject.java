package net.hollowcube.mapmaker.map.script.friendly;

import net.hollowcube.luau.LuaState;
import org.jetbrains.annotations.NotNull;

public interface LuaObject {

    default @NotNull String luaTypeName() {
        return getClass().getName();
    }

    default void close(@NotNull LuaState state) {

    }
}
