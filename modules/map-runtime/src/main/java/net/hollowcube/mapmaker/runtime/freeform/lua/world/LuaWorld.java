package net.hollowcube.mapmaker.runtime.freeform.lua.world;

import net.hollowcube.luau.LuaState;
import net.hollowcube.mapmaker.runtime.freeform.FreeformMapWorld;
import net.hollowcube.mapmaker.runtime.freeform.lua.math.LuaVectorTypeImpl;

import static net.hollowcube.mapmaker.runtime.freeform.script.LuaHelpers.noSuchKey;
import static net.hollowcube.mapmaker.runtime.freeform.script.LuaHelpers.noSuchMethod;

public class LuaWorld {
    private static final String NAME = "World";

    public static void init(LuaState state) {
        // Create the metatable for Entity
        state.newMetaTable(NAME);
        state.pushCFunction(LuaWorld::luaIndex, "__index");
        state.setField(-2, "__index");
        state.pushCFunction(LuaWorld::luaNewIndex, "__newindex");
        state.setField(-2, "__newindex");
        state.pushCFunction(LuaWorld::luaNameCall, "__namecall");
        state.setField(-2, "__namecall");
        state.pop(1);
    }

    public static void push(LuaState state, LuaWorld entity) {
        state.newUserData(entity);
        state.getMetaTable(NAME);
        state.setMetaTable(-2);
    }

    public static LuaWorld checkArg(LuaState state, int index) {
        return (LuaWorld) state.checkUserDataArg(index, NAME);
    }

    private final FreeformMapWorld delegate;

    public LuaWorld(FreeformMapWorld world) {
        this.delegate = world;
    }

    // Properties

    private int getUuid(LuaState state) {
        state.pushString(delegate.map().id());
        return 1;
    }

    // Methods

    private int getBlock(LuaState state) {
        var blockPosition = LuaVectorTypeImpl.checkArg(state, 1);

        var block = delegate.instance().getBlock(blockPosition);
        LuaBlock.push(state, block);
        return 1;
    }

    private int setBlock(LuaState state) {
        var blockPosition = LuaVectorTypeImpl.checkArg(state, 1);
        var block = LuaBlock.checkArg(state, 2);

        delegate.instance().setBlock(blockPosition, block);
        return 0;
    }

    // Metamethods

    private static int luaIndex(LuaState state) {
        final LuaWorld world = checkArg(state, 1);
        final String key = state.checkStringArg(2);
        return switch (key) {
            case "Uuid" -> world.getUuid(state);
            default -> noSuchKey(state, NAME, key);
        };
    }

    private static int luaNewIndex(LuaState state) {
        final LuaWorld world = checkArg(state, 1);
        final String key = state.checkStringArg(2);
        state.remove(1); // Remove the userdata from the stack
        state.remove(1); // Remove the key from the stack
        return switch (key) {
            default -> noSuchKey(state, NAME, key);
        };
    }

    private static int luaNameCall(LuaState state) {
        final LuaWorld world = checkArg(state, 1);
        state.remove(1); // Remove the world userdata from the stack (so implementations can pretend they have no self)
        final String methodName = state.nameCallAtom();
        return switch (methodName) {
            case "GetBlock" -> world.getBlock(state);
            case "SetBlock" -> world.setBlock(state);
            default -> noSuchMethod(state, NAME, methodName);
        };
    }

}