package net.hollowcube.mapmaker.runtime.freeform.lua.entity;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.annotation.LuaProperty;
import net.hollowcube.luau.annotation.LuaType;
import net.hollowcube.mapmaker.runtime.freeform.lua.math.LuaVectorTypeImpl;
import net.minestom.server.entity.Entity;

@LuaType
public class LuaEntity implements LuaEntity$luau {

    public static void push(LuaState state, LuaEntity entity) {
        state.newUserData(entity);
        state.getMetaTable(TYPE_NAME);
        state.setMetaTable(-2);
    }

    public static <E extends LuaEntity> E checkArg(LuaState state, int index, Class<E> type) {
        var entity = (LuaEntity) state.checkUserDataArg(index, TYPE_NAME);
        if (!type.isAssignableFrom(entity.getClass())) {
            state.argError(index, "Expected " + type.getSimpleName() +
                    ", got " + entity.getClass().getSimpleName());
        }
        return type.cast(entity);
    }

    public static LuaEntity checkArg(LuaState state, int index) {
        return (LuaEntity) state.checkUserDataArg(index, TYPE_NAME);
    }

    private final Entity delegate;

    public LuaEntity(Entity delegate) {
        this.delegate = delegate;
    }

    protected Entity delegate() {
        return this.delegate;
    }

    public boolean readField(LuaState state, String key, int index) {
        return switch (key) {
            default -> false;
        };
    }

    //region Properties

    @LuaProperty
    public int getUuid(LuaState state) {
        state.pushString(delegate.getUuid().toString());
        return 1;
    }

    @LuaProperty
    public int getPosition(LuaState state) {
        LuaVectorTypeImpl.push(state, delegate().getPosition());
        return 1;
    }

    @LuaProperty
    public int getYaw(LuaState state) {
        state.pushNumber(delegate().getPosition().yaw());
        return 1;
    }

    @LuaProperty
    public int getPitch(LuaState state) {
        state.pushNumber(delegate().getPosition().pitch());
        return 1;
    }

    //endregion

    //region Instance Methods

    public int remove(LuaState state) {
        if (delegate.isRemoved())
            return 0;
        delegate.remove();
        return 0;
    }

    //endregion

}
