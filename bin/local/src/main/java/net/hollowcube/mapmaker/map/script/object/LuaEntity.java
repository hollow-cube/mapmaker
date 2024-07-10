package net.hollowcube.mapmaker.map.script.object;

import net.hollowcube.luau.annotation.LuaMethod;
import net.hollowcube.luau.annotation.LuaObject;
import net.hollowcube.luau.annotation.LuaProperty;
import net.hollowcube.luau.util.Pinned;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Entity;
import org.jetbrains.annotations.NotNull;

@LuaObject
public class LuaEntity implements Pinned {

    private final Entity entity;

    public LuaEntity(@NotNull Entity entity) {
        this.entity = entity;
    }

    @LuaProperty
    public @NotNull Point getPosition() {
        return entity.getPosition();
    }

    @LuaMethod
    public void remove() {
        entity.remove();
    }

    @Override
    public void unpin() {
        entity.remove();
    }
}
