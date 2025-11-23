package net.hollowcube.mapmaker.scripting.util;

import net.hollowcube.luau.LuaState;

import java.util.function.Consumer;

public final class LuaHelpers {

    /// Iterates over a table (no checks to ensure its a table) and applies the given function for each key.
    /// During the callback, the value is always at index -1 (and the key at -2 if needed).
    ///
    /// The state should be left _exactly_ as it was before the call (value at -1).
    public static void tableForEach(LuaState state, int tableIndex, Consumer<String> func) {
        state.pushNil();
        while (state.next(tableIndex)) {
            // Key is at index -2, value is at index -1
            String key = state.toString(-2);
            func.accept(key);

            // Remove the value, keep the key for the next iteration
            state.pop(1);
        }
    }

    /// Returns true if the key exists, it is at the top of the stack.
    public static boolean tableGet(LuaState state, int tableIndex, String key) {
        state.getField(tableIndex, key);
        if (state.isNil(-1)) {
            state.pop(1); // Remove nil
            return false;
        }
        return true;
    }

}
