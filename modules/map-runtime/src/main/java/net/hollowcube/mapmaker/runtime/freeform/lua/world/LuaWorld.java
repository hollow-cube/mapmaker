package net.hollowcube.mapmaker.runtime.freeform.lua.world;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.annotation.LuaProperty;
import net.hollowcube.luau.annotation.LuaType;
import net.hollowcube.mapmaker.runtime.freeform.FreeformMapWorld;
import net.hollowcube.mapmaker.runtime.freeform.lua.math.LuaVectorTypeImpl;

@LuaType
public class LuaWorld implements LuaWorld$luau {

    public static void push(LuaState state, LuaWorld entity) {
        state.newUserData(entity);
        state.getMetaTable(TYPE_NAME);
        state.setMetaTable(-2);
    }

    public static LuaWorld checkArg(LuaState state, int index) {
        return (LuaWorld) state.checkUserDataArg(index, TYPE_NAME);
    }

    private final FreeformMapWorld delegate;

    public LuaWorld(FreeformMapWorld world) {
        this.delegate = world;
    }

    //region Instance Properties

    @LuaProperty
    public int getUuid(LuaState state) {
        state.pushString(delegate.map().id());
        return 1;
    }

    //endregion

    //region Instance Methods

    public int getBlock(LuaState state) {
        var blockPosition = LuaVectorTypeImpl.checkArg(state, 1);

        var block = delegate.instance().getBlock(blockPosition);
        LuaBlockImpl.push(state, block);
        return 1;
    }

    public int setBlock(LuaState state) {
        var blockPosition = LuaVectorTypeImpl.checkArg(state, 1);
        var block = LuaBlockImpl.checkArg(state, 2);

        delegate.instance().setBlock(blockPosition, block);
        return 0;
    }

    //endregion

}