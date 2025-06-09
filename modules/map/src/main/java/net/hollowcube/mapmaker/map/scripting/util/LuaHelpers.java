package net.hollowcube.mapmaker.map.scripting.util;

import net.hollowcube.luau.LuaState;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public class LuaHelpers {

    public static int noSuchKey(@NotNull LuaState state, @NotNull String typeName, @NotNull String methodName) {
        state.error("No such key '" + methodName + "' for " + typeName);
        return 0; // Never reached, just to make java happy
    }

    public static int noSuchMethod(@NotNull LuaState state, @NotNull String typeName, @NotNull String methodName) {
        state.error("No such method '" + methodName + "' for " + typeName);
        return 0; // Never reached, just to make java happy
    }

    public static @NotNull Key checkKeyArg(@NotNull LuaState state, int index) {
        var key = state.checkStringArg(index);
        try {
            return Key.key(key);
        } catch (InvalidKeyException e) {
            state.error("Invalid key: " + key);
            return null; // Never reached, just to make java happy
        }
    }

}
