package net.hollowcube.mapmaker.runtime.freeform.script;

import com.google.gson.*;
import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.LuaType;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;

import java.util.function.Consumer;

public class LuaHelpers {

    public static int noSuchKey(LuaState state, String typeName, String methodName) {
        state.error("No such key '" + methodName + "' for " + typeName);
        return 0; // Never reached, just to make java happy
    }

    public static int noSuchMethod(LuaState state, String typeName, String methodName) {
        state.error("No such method '" + methodName + "' for " + typeName);
        return 0; // Never reached, just to make java happy
    }

    public static int fieldReadOnly(LuaState state, String typeName, String key) {
        state.error(typeName + "." + key + " is read-only");
        return 0;
    }

    public static int fieldWriteOnly(LuaState state, String typeName, String key) {
        state.error(typeName + "." + key + " is write-only");
        return 0;
    }

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

    // Returns true if the key exists, it is at the top of the stack.
    public static boolean tableGet(LuaState state, int tableIndex, String key) {
        state.getField(tableIndex, key);
        if (state.isNil(-1)) {
            state.pop(1); // Pop the nil value
            return false;
        }
        return true;
    }

    public static Key checkKeyArg(LuaState state, int index) {
        var key = state.checkStringArg(index);
        try {
            return Key.key(key);
        } catch (InvalidKeyException e) {
            state.error("Invalid key: " + key);
            return null; // Never reached, just to make java happy
        }
    }

    public static float[] checkFloat4Arg(LuaState state, int index) {
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

    public static void pushFloat4(LuaState state, float[] floats) {
        if (floats.length != 4) throw new IllegalArgumentException("Float4 must have exactly 4 elements");
        state.newTable();
        for (int i = 0; i < 4; i++) {
            state.pushNumber(floats[i]);
            state.rawSetI(-2, i + 1);
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

    public static JsonElement readJsonElement(LuaState state, int index) {
        return switch (state.type(index)) {
            case NIL -> JsonNull.INSTANCE;
            case BOOLEAN -> new JsonPrimitive(state.toBoolean(-1));
            case NUMBER -> new JsonPrimitive(state.toNumber(-1));
            case STRING -> new JsonPrimitive(state.toString(-1));
            case TABLE -> {
                // TODO: support arrays.
                var obj = new JsonObject();
                tableForEach(state, index - 1, key -> obj.add(key, readJsonElement(state, -1)));
                yield obj;
            }
            // todo support vector type, some userdata types, and buffer type (probably)
            case NONE, DEADKEY, UPVAL, PROTO, FUNCTION, LIGHTUSERDATA, USERDATA, VECTOR, THREAD, BUFFER -> {
                throw new IllegalArgumentException("Cannot read JSON from type " + state.typeName(index));
            }
        };
    }

}

