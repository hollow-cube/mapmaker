package net.hollowcube.luau.util;

import net.hollowcube.luau.LuaState;
import org.jetbrains.annotations.NotNull;

public final class LuaHelpers {
//    private static final ClassValue<> ABC = new ClassValue<>() {
//    };


    public static boolean isUserData(@NotNull LuaState state, int index, @NotNull String name) {
        int found = state.getMetaTable(index);
        if (found == 0) return false;
        state.getMetaTable(name);
        boolean result = state.rawEqual(-1, -2);
        state.pop(2); //pop both
        return result;
    }

    public static <T> T checkUserDataArg(@NotNull LuaState state, int index, @NotNull Class<T> requiredType) {
        if (state.isUserData(index)) {
            Object ud = state.toUserData(index);
            if (requiredType.isAssignableFrom(ud.getClass())) {
                return requiredType.cast(ud);
            }
        }
        // Note that we do leave some values on the stack here, but it doesnt really matter because this error will unwind the function anyway.
        state.typeError(index, requiredType.getName()); //todo getName is not right here
        return null;
    }

}
