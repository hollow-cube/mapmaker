package net.hollowcube.luau.func;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.util.Pin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

public final class LuaFunctions {
    private static final Map<Class<?>, LuaBinder> BINDERS;

    static {
        var map = new HashMap<Class<?>, LuaBinder>();
        ServiceLoader.load(LuaBinder.class).forEach(binder -> map.put(binder.target(), binder));
        BINDERS = Map.copyOf(map);
    }

    public static <F> @NotNull Pin<F> bind(@NotNull Class<F> functionType, @NotNull LuaState state, int index) {
        var binder = BINDERS.get(functionType);
        if (binder != null) return (Pin<F>) binder.bind(state, index);
        throw new UnsupportedOperationException("No binder found for " + functionType);
    }

}
