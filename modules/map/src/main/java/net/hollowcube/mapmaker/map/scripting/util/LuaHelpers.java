package net.hollowcube.mapmaker.map.scripting.util;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.LuaType;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class LuaHelpers {

    public static int noSuchKey(@NotNull LuaState state, @NotNull String typeName, @NotNull String methodName) {
        state.error("No such key '" + methodName + "' for " + typeName);
        return 0; // Never reached, just to make java happy
    }

    public static int noSuchMethod(@NotNull LuaState state, @NotNull String typeName, @NotNull String methodName) {
        state.error("No such method '" + methodName + "' for " + typeName);
        return 0; // Never reached, just to make java happy
    }

    /// Iterates over a table (no checks to ensure its a table) and applies the given function for each key.
    /// During the callback, the value is always at index -1 (and the key at -2 if needed).
    ///
    /// The state should be left _exactly_ as it was before the call (value at -1).
    public static void tableForEach(@NotNull LuaState state, int tableIndex, @NotNull Consumer<String> func) {
        state.pushNil();
        while (state.next(2)) {
            // Key is at index -2, value is at index -1
            String key = state.toString(-2);
            func.accept(key);

            // Remove the value, keep the key for the next iteration
            state.pop(1);
        }
    }

    // Returns true if the key exists, it is at the top of the stack.
    public static boolean tableGet(@NotNull LuaState state, int tableIndex, @NotNull String key) {
        state.getField(tableIndex, key);
        if (state.isNil(-1)) {
            state.pop(1); // Pop the nil value
            return false;
        }
        return true;
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

    public static float[] checkFloat4Arg(@NotNull LuaState state, int index) {
        state.checkType(index, LuaType.TABLE);
        float[] floats = new float[4];
        for (int i = 0; i < 4; i++) {
            state.rawGetI(index, i + 1);
            if (state.isNumber(-1)) {
                floats[i] = (float) state.toNumber(-1);
            } else {
                state.argError(index, "Expected a number at index " + (i + 1));
            }
            state.pop(1); // Pop the value
        }
        return floats;
    }

    public static void pushFloat4(@NotNull LuaState state, float[] floats) {
        if (floats.length != 4) throw new IllegalArgumentException("Float4 must have exactly 4 elements");
        state.newTable();
        for (int i = 0; i < 4; i++) {
            state.pushNumber(floats[i]);
            state.rawSetI(-2, i + 1);
        }
    }

}
