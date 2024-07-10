package net.hollowcube.luau.func;

import net.hollowcube.luau.LuaState;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

// SPI Type
@ApiStatus.Internal
public interface LuaBinder {

    @NotNull Class<?> target();

    @NotNull Object bind(@NotNull LuaState state, int index);

}
