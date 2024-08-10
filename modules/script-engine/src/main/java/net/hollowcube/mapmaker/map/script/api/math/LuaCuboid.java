package net.hollowcube.mapmaker.map.script.api.math;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.annotation.LuaMethod;
import net.hollowcube.luau.annotation.LuaObject;
import net.hollowcube.luau.annotation.LuaProperty;
import net.hollowcube.mapmaker.map.script.api.entity.LuaEntity;
import net.hollowcube.mapmaker.util.CoordinateUtil;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Entity;
import org.jetbrains.annotations.NotNull;

/**
 * I am a cuboid
 */
@LuaObject
public class LuaCuboid {

    public static void init(@NotNull LuaState state) {
        LuaCuboid$Wrapper.initMetatable(state);
    }

    @LuaMethod(name = "new")
    public static @NotNull LuaCuboid newCuboid(@NotNull Point pos1, @NotNull Point pos2) {
        return new LuaCuboid(pos1, pos2);
    }

    /**
     * <p>Hello, world</p>
     *
     * <code>
     * Lua.run("print('Hello, world!')");
     * </code>
     */
    @LuaProperty
    public final Point min;
    /**
     * <p>Hello, world</p>
     */
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
    public boolean contains(@NotNull Point point) {
        return CoordinateUtil.isBetween(min, max, point);
    }

    @LuaMethod
    public boolean intersects(@NotNull LuaCuboid other) {
        return CoordinateUtil.intersects(min, max, other.min, other.max);
    }

    @LuaMethod
    public boolean intersects(@NotNull LuaEntity other) {
        return intersects(other.getBoundingBox());
    }

}
