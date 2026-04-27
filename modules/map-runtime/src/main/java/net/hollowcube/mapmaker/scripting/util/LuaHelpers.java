package net.hollowcube.mapmaker.scripting.util;

import com.google.gson.*;
import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.internal.vm.lua_Debug;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.jetbrains.annotations.Nullable;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.List;
import java.util.function.Consumer;

import static net.hollowcube.luau.internal.vm.lua_h.lua_getinfo;

public final class LuaHelpers {
    private static final MemorySegment DEBUG_WHAT = Arena.global().allocateFrom("s");

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

    /// Iterates over an array (sequence) table in order from index 1 to #table.
    /// During the callback, the value is at index -1.
    /// The state is left exactly as it was before the call.
    public static void arrayForEach(LuaState state, int tableIndex, Consumer<Integer> func) {
        int absIndex = state.absIndex(tableIndex);
        int length = state.len(absIndex);
        for (int i = 1; i <= length; i++) {
            state.rawGetI(absIndex, i);
            func.accept(i);
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

    public static @Nullable Key checkOptKey(LuaState state, int index) {
        if (state.isNil(index)) return null;
        return checkKey(state, index);
    }

    public static Key checkKey(LuaState state, int index) {
        var key = state.checkString(index);
        if (!Key.parseable(key)) throw state.error("Invalid key: " + key);
        return Key.key(key);
    }

    public static @Nullable Sound.Source checkOptSoundCategory(LuaState state, int index) {
        if (state.isNil(index)) return null;
        return checkSoundCategory(state, index);
    }

    public static Sound.Source checkSoundCategory(LuaState state, int index) {
        class Holder {
            static final List<String> CATEGORIES = List.copyOf(Sound.Source.NAMES.keys());
        }

        var nameIndex = state.checkOption(index, Sound.Source.MASTER.name(), Holder.CATEGORIES);
        return Sound.Source.NAMES.valueOr(Holder.CATEGORIES.get(nameIndex), Sound.Source.MASTER);
    }

    // todo: should be a method on LuaState in luau-java probably, its useful
    public static String currentChunkName(LuaState state) {
        try (Arena arena = Arena.ofConfined()) {
            final MemorySegment debug = lua_Debug.allocate(arena);
            int level = 0;
            do {
                if (lua_getinfo(state.L(), level++, DEBUG_WHAT, debug) == 0)
                    throw state.error("not supported in this context");
            } while (lua_Debug.what(debug).get(ValueLayout.JAVA_BYTE, 0) != 'L');

            return lua_Debug.source(debug).getString(0);
        }
    }

    public static void pushJsonElement(LuaState state, JsonElement element) {
        switch (element) {
            case JsonObject object -> {
                state.newTable();
                for (var entry : object.entrySet()) {
                    pushJsonElement(state, entry.getValue());
                    state.setField(-2, entry.getKey());
                }
            }
            case JsonArray array -> {
                state.newTable();
                for (int i = 0; i < array.size(); i++) {
                    pushJsonElement(state, array.get(i));
                    state.rawSetI(-2, i + 1);
                }
            }
            case JsonNull _ -> state.pushNil();
            case JsonPrimitive primitive -> {
                if (primitive.isBoolean()) {
                    state.pushBoolean(primitive.getAsBoolean());
                } else if (primitive.isNumber()) {
                    state.pushNumber(primitive.getAsDouble());
                } else {
                    state.pushString(primitive.getAsString());
                }
            }
            default -> throw new IllegalArgumentException("Unknown JsonElement type: " + element.getClass());
        }
    }

    public static JsonElement toJsonElement(LuaState state, int index) {
        return switch (state.type(index)) {
            case NIL -> JsonNull.INSTANCE;
            case BOOLEAN -> new JsonPrimitive(state.toBoolean(-1));
            case NUMBER -> new JsonPrimitive(state.toNumber(-1));
            case STRING -> new JsonPrimitive(state.toString(-1));
            case TABLE -> {
                int absIndex = state.absIndex(index);
                var obj = new JsonObject();
                tableForEach(state, absIndex, key -> obj.add(key, toJsonElement(state, -1)));
                yield obj;
            }
            // todo support vector type, some userdata types, and buffer type (probably)
            case NONE, FUNCTION, LIGHTUSERDATA, USERDATA, VECTOR, THREAD, BUFFER -> {
                throw new IllegalArgumentException("Cannot read JSON from type " + state.typeName(index));
            }
        };
    }

}
