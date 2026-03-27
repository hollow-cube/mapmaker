package net.hollowcube.mapmaker.scripting.api;

import net.hollowcube.luau.LuaFunc;
import net.hollowcube.luau.LuaState;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;

public final class LuaVector {
    private static final LuaFunc INDEX = LuaFunc.wrap(LuaVector::luaIndex, "__index");
    private static final LuaFunc TOSTRING = LuaFunc.wrap(LuaVector::luaToString, "__tostring");
    private static final LuaFunc ADD = LuaFunc.wrap(LuaVector::luaAdd, "__add");
    private static final LuaFunc SUB = LuaFunc.wrap(LuaVector::luaSub, "__sub");

    public static void register(LuaState state) {
        // Put a zero vector on the stack, we will eventually assign the metatable to it
        state.pushVector(0f, 0f, 0f);

        // Create metatable
        state.newMetaTable("vector");
        state.pushFunction(INDEX);
        state.setField(-2, "__index");
        state.pushFunction(TOSTRING);
        state.setField(-2, "__tostring");
        state.pushFunction(ADD);
        state.setField(-2, "__add");
        state.pushFunction(SUB);
        state.setField(-2, "__sub");

        // Assign to the zero vector and pop (aka all vectors)
        state.setReadOnly(-1, true);
        state.setMetaTable(-2);
        state.pop(1);

        // Constructor is omitted, its a compiler intrinsic
    }

    public static void push(LuaState state, Point point) {
        state.pushVector((float) point.x(), (float) point.y(), (float) point.z());
    }

    public static Point check(LuaState state, int index) {
        float[] raw = state.checkVector(index);
        return new Vec(raw[0], raw[1], raw[2]);
    }

    private static int luaIndex(LuaState state) {
        var vec = state.checkVector(1);
        // TODO: should use string atoms here, but the slop generator doesnt support this pattern yet, so strings for now.
        var name = state.checkString(2);

        if ("length".equals(name)) {
            state.pushNumber(Math.sqrt(vec[0] * vec[0] + vec[1] * vec[1] + vec[2] * vec[2]));
            return 1;
        }

        int elem = name.charAt(0) - 'X';
        if (elem < 0 || elem > 2)
            throw state.error("No such key: " + name);
        state.pushNumber(vec[elem]);
        return 1;
    }

    private static int luaToString(LuaState state) {
        var vec = state.checkVector(1);
        state.pushString(String.format("vec(%.2g, %.2g, %.2g)", vec[0], vec[1], vec[2]));
        return 1;
    }

    private static int luaAdd(LuaState state) {
        var lhs = state.checkVector(1);
        var rhsType = state.type(2);
        switch (rhsType) {
            case NUMBER -> {
                var n = (float) state.checkNumber(2);
                state.pushVector(lhs[0] + n, lhs[1] + n, lhs[2] + n);
            }
            case VECTOR -> {
                var rhs = state.checkVector(2);
                state.pushVector(lhs[0] + rhs[0], lhs[1] + rhs[1], lhs[2] + rhs[2]);
            }
            default -> state.typeError(1, "number or vector");
        }
        return 1;
    }

    private static int luaSub(LuaState state) {
        var lhs = state.checkVector(1);
        var rhsType = state.type(2);
        switch (rhsType) {
            case NUMBER -> {
                var n = (float) state.checkNumber(2);
                state.pushVector(lhs[0] - n, lhs[1] - n, lhs[2] - n);
            }
            case VECTOR -> {
                var rhs = state.checkVector(2);
                state.pushVector(lhs[0] - rhs[0], lhs[1] - rhs[1], lhs[2] - rhs[2]);
            }
            default -> state.typeError(2, "number or vector");
        }
        return 1;
    }

    //todo other metamethods
}
