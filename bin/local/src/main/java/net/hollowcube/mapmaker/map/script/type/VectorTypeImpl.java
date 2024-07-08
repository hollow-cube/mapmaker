package net.hollowcube.mapmaker.map.script.type;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.annotation.LuaMeta;
import net.hollowcube.luau.annotation.LuaTypeImpl;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.NotNull;

/**
 * <p>Implements a metatable and type conversion from the builtin Luau vector primitive
 * to the Minestom Point class.</p>
 *
 * <p>These functions generally operate on a raw lua state rather than using conversion
 * to the Point class. No need to make the garbage for simple functions like these.</p>
 */
@LuaTypeImpl(Point.class)
public final class VectorTypeImpl {
    private static final float[] ZERO = new float[]{0.0f, 0.0f, 0.0f};

    public static void pushValue(@NotNull LuaState state, @NotNull Point point) {
        state.pushVector((float) point.x(), (float) point.y(), (float) point.z());
    }

    public static @NotNull Point checkArg(@NotNull LuaState state, int index) {
        float[] raw = state.checkVectorArg(index);
        return new Vec(raw[0], raw[1], raw[2]);
    }

    @LuaMeta(LuaMeta.Type.INDEX)
    static int luaIndex(@NotNull LuaState state) {
        var vec = state.checkVectorArg(1);
        var name = state.checkStringArg(2);

        int elem = name.charAt(0) - 'x';
        if (elem < 0 || elem > 2) {
            state.error("No such key: " + name);
            return 0;
        }

        state.pushNumber(vec[elem]);
        return 1;
    }

    @LuaMeta(LuaMeta.Type.TOSTRING)
    static int luaToString(@NotNull LuaState state) {
        var vec = state.checkVectorArg(1);
        state.pushString(String.format("vec(%f, %f, %f)", vec[0], vec[1], vec[2]));
        return 1;
    }

    @LuaMeta(LuaMeta.Type.ADD)
    static int luaAdd(@NotNull LuaState state) {
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

    @LuaMeta(LuaMeta.Type.SUB)
    static int luaSub(@NotNull LuaState state) {
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

    private VectorTypeImpl() {
    }

}
