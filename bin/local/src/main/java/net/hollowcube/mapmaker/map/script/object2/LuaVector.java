package net.hollowcube.mapmaker.map.script.object2;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.annotation.LuaTypeImpl;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.NotNull;

@LuaTypeImpl(Point.class)
public class LuaVector {

    public static void pushValue(@NotNull LuaState state, @NotNull Point point) {
        state.pushVector((float) point.x(), (float) point.y(), (float) point.z());
    }

    public static @NotNull Point checkArg(@NotNull LuaState state, int index) {
        float[] raw = state.checkVectorArg(index);
        return new Vec(raw[0], raw[1], raw[2]);
    }

}
