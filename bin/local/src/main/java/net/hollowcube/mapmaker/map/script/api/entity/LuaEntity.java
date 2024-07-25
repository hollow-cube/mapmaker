package net.hollowcube.mapmaker.map.script.api.entity;

import net.hollowcube.luau.annotation.LuaMethod;
import net.hollowcube.luau.annotation.LuaObject;
import net.hollowcube.luau.annotation.LuaProperty;
import net.hollowcube.mapmaker.map.script.api.math.LuaCuboid;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Entity;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

@LuaObject
public class LuaEntity {
    public static final Tag<Boolean> SCRIPT_SPAWNED = Tag.<Boolean>Transient("script_spawned").defaultValue(false);

    protected final Entity entity;

    public LuaEntity(@NotNull Entity entity) {
        this.entity = entity;
    }

    @LuaProperty
    public @NotNull Point getPosition() {
        return entity.getPosition();
    }

    @LuaProperty
    public @NotNull LuaCuboid getBoundingBox() {
        return new LuaCuboid(entity);
    }

    @LuaMethod
    public void setInvisible(boolean invisible) {
        entity.setInvisible(invisible);
    }

    @LuaMethod
    public void remove() {
        if (!entity.hasTag(SCRIPT_SPAWNED))
            throw new IllegalStateException("Only entities spawned by scripts may be removed.");
        entity.remove();
    }

}
