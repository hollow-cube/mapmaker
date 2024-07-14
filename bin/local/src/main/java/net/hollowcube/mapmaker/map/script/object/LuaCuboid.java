package net.hollowcube.mapmaker.map.script.object;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.annotation.LuaMethod;
import net.hollowcube.luau.annotation.LuaObject;
import net.hollowcube.luau.annotation.LuaProperty;
import net.hollowcube.mapmaker.map.script.type.VectorTypeImpl;
import net.hollowcube.mapmaker.util.CoordinateUtil;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Entity;
import org.jetbrains.annotations.NotNull;

@LuaObject
public class LuaCuboid {

    public static void init(@NotNull LuaState state) {
        LuaCuboid$Wrapper.initMetatable(state);

        state.newTable();
        state.pushCFunction(LuaCuboid::newCuboid, "new");
        state.setField(-2, "new");
        state.setReadOnly(-1, true);
        state.setGlobal("Cuboid");
    }

    private static int newCuboid(@NotNull LuaState state) {
        var pos1 = VectorTypeImpl.checkLuaArg(state, 1);
        var pos2 = VectorTypeImpl.checkLuaArg(state, 2);

        state.newUserData(new LuaCuboid(pos1, pos2));
        state.getMetaTable(LuaCuboid$Wrapper.TYPE_NAME);
        state.setMetaTable(-2);

        return 1;
    }

    @LuaProperty
    public final Point min;
    @LuaProperty
    public final Point max;

    public LuaCuboid(@NotNull Point pos1, @NotNull Point pos2) {
        this.min = CoordinateUtil.min(pos1, pos2);
        this.max = CoordinateUtil.max(pos1, pos2);
    }

    public LuaCuboid(@NotNull Entity entity) {
        this(entity.getPosition().add(entity.relativeStart()), entity.getPosition().add(entity.relativeEnd()));
    }

    @LuaMethod
    public int contains(@NotNull LuaState state) {
        var point = VectorTypeImpl.checkLuaArg(state, 1);
        state.pushBoolean(CoordinateUtil.isBetween(min, max, point));
        return 1;
    }

    @LuaMethod
    public int intersects(@NotNull LuaState state) {
        //todo properly support overloads
        LuaCuboid other = null;
        if (state.isUserData(1)) {
            var arg1 = state.toUserData(1);
            if (arg1 instanceof LuaCuboid c) other = c;
            else if (arg1 instanceof LuaEntity e) other = e.getBoundingBox();
            else if (arg1 instanceof LuaPlayer p) other = p.getBoundingBox();
        }
        if (other == null) {
            state.argError(1, "expected entity, got " + state.typeName(1));
            return 0;
        }

        state.pushBoolean(intersects0(other));
        return 1;
    }

    public boolean intersects0(@NotNull LuaCuboid other) {
        return CoordinateUtil.intersects(min, max, other.min, other.max);
    }

}
