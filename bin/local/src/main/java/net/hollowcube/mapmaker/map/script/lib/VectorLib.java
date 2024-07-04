package net.hollowcube.mapmaker.map.script.lib;

import net.hollowcube.luau.LuaState;
import org.jetbrains.annotations.NotNull;

public class VectorLib {
    private static final float[] ZERO = new float[]{0.0f, 0.0f, 0.0f};

    public static void open(@NotNull LuaState state) {
        state.pushVector(ZERO);
        state.newMetaTable("vector");

        state.pushCFunction(VectorLib::luaIndex, "__index");
        state.setField(-2, "__index");

        state.pushCFunction(VectorLib::luaNameCall, "__namecall");
        state.setField(-2, "__namecall");

        state.pushCFunction(VectorLib::luaAdd, "__add");
        state.setField(-2, "__add");
        state.pushCFunction(VectorLib::luaSub, "__sub");
        state.setField(-2, "__sub");

        state.pushCFunction(VectorLib::luaToString, "__tostring");
        state.setField(-2, "__tostring");

        state.setReadOnly(-1, true);
        state.setMetaTable(-2);
        state.pop(1); // Remove the empty vector from stack

        state.pushCFunction(ls -> {
            double x = ls.checkNumberArg(1);
            double y = ls.checkNumberArg(2);
            double z = ls.checkNumberArg(3);
            ls.pushVector((float) x, (float) y, (float) z);
            return 1;
        }, "vec");
        state.setGlobal("vec");
    }

    private static int luaIndex(@NotNull LuaState state) {
        var vec = state.checkVectorArg(1);
        var name = state.checkStringArg(2);

        return switch (name) {
            case "x" -> {
                state.pushNumber(vec[0]);
                yield 1;
            }
            case "y" -> {
                state.pushNumber(vec[1]);
                yield 1;
            }
            case "z" -> {
                state.pushNumber(vec[2]);
                yield 1;
            }
            default -> {
                state.error("No such key: " + name);
                yield 0; // Never reached
            }
        };
    }

    private static int luaNameCall(@NotNull LuaState state) {
        state.error("No such method: " + state.checkStringArg(1));
        return 0;
    }

    private static int luaToString(@NotNull LuaState state) {
        var vec = state.checkVectorArg(1);
        state.pushString(String.format("Vec(%f, %f, %f)", vec[0], vec[1], vec[2]));
        return 1;
    }

    public static int luaAdd(@NotNull LuaState state) {
        var vec1 = state.checkVectorArg(1);
        if (state.isVector(2)) {
            var vec2 = state.checkVectorArg(2);
            state.pushVector(vec1[0] + vec2[0], vec1[1] + vec2[1], vec1[2] + vec2[2]);
        } else {
            var n = (float) state.checkNumberArg(2);
            state.pushVector(vec1[0] + n, vec1[1] + n, vec1[2] + n);
        }
        return 1;
    }

    private static int luaSub(@NotNull LuaState state) {
        var vec1 = state.checkVectorArg(1);
        var vec2 = state.checkVectorArg(2);
        state.pushVector(vec1[0] - vec2[0], vec1[1] - vec2[1], vec1[2] - vec2[2]);
        return 1;
    }
}
