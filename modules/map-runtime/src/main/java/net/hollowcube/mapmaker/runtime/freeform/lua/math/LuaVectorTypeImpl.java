package net.hollowcube.mapmaker.runtime.freeform.lua.math;

import net.hollowcube.luau.LuaState;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;

public final class LuaVectorTypeImpl {
    public static final String NAME = "vector";

    public static void init(LuaState state) {
        // Put a zero vector on the stack, we will eventually assign the metatable to it
        state.pushVector(0f, 0f, 0f);

        // Create metatable
        state.newMetaTable(NAME);
        state.pushCFunction(LuaVectorTypeImpl::luaIndex, "__index");
        state.setField(-2, "__index");
        state.pushCFunction(LuaVectorTypeImpl::luaToString, "__tostring");
        state.setField(-2, "__tostring");
        state.pushCFunction(LuaVectorTypeImpl::luaAdd, "__add");
        state.setField(-2, "__add");
        state.pushCFunction(LuaVectorTypeImpl::luaSub, "__sub");
        state.setField(-2, "__sub");

        // Assign to the zero vector and pop (aka all vectors)
        state.setMetaTable(-2);
        state.pop(1);

        // Create constructor
        state.pushCFunction(LuaVectorTypeImpl::vectorCtor, "vec");
        state.setGlobal("vec");
    }

    public static void push(LuaState state, Point point) {
        state.pushVector((float) point.x(), (float) point.y(), (float) point.z());
    }

    public static Point checkArg(LuaState state, int index) {
        float[] raw = state.checkVectorArg(index);
        return new Vec(raw[0], raw[1], raw[2]);
    }

    private static int vectorCtor(LuaState state) {
        double x = state.checkNumberArg(1);
        double y = state.checkNumberArg(2);
        double z = state.checkNumberArg(3);
        state.pushVector((float) x, (float) y, (float) z);
        return 1;
    }

    static int luaIndex(LuaState state) {
        var vec = state.checkVectorArg(1);
        var name = state.checkStringArg(2);

        if ("Length".equals(name)) {
            state.pushNumber(Math.sqrt(vec[0] * vec[0] + vec[1] * vec[1] + vec[2] * vec[2]));
            return 1;
        }

        int elem = name.charAt(0) - 'X';
        if (elem < 0 || elem > 2) {
            state.error("No such key: " + name);
            return 0;
        }

        state.pushNumber(vec[elem]);
        return 1;
    }

    static int luaToString(LuaState state) {
        var vec = state.checkVectorArg(1);
        state.pushString(String.format("vec(%f, %f, %f)", vec[0], vec[1], vec[2]));
        return 1;
    }

    static int luaAdd(LuaState state) {
        var lhs = state.checkVectorArg(1);
        var rhsType = state.type(2);
        switch (rhsType) {
            case NUMBER -> {
                var n = (float) state.checkNumberArg(2);
                state.pushVector(lhs[0] + n, lhs[1] + n, lhs[2] + n);
            }
            case VECTOR -> {
                var rhs = state.checkVectorArg(2);
                state.pushVector(lhs[0] + rhs[0], lhs[1] + rhs[1], lhs[2] + rhs[2]);
            }
            default -> state.error("Expected number or vector, got " + state.typeName(2));
        }
        return 1;
    }

    static int luaSub(LuaState state) {
        var lhs = state.checkVectorArg(1);
        var rhsType = state.type(2);
        switch (rhsType) {
            case NUMBER -> {
                var n = (float) state.checkNumberArg(2);
                state.pushVector(lhs[0] - n, lhs[1] - n, lhs[2] - n);
            }
            case VECTOR -> {
                var rhs = state.checkVectorArg(2);
                state.pushVector(lhs[0] - rhs[0], lhs[1] - rhs[1], lhs[2] - rhs[2]);
            }
            default -> state.error("Expected number or vector, got " + state.typeName(2));
        }
        return 1;
    }

    //todo other metamethods
}

