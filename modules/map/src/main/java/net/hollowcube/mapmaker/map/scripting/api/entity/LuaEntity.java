package net.hollowcube.mapmaker.map.scripting.api.entity;

import net.hollowcube.luau.LuaState;
import net.hollowcube.mapmaker.map.scripting.api.math.LuaVectorTypeImpl;
import net.minestom.server.entity.Entity;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.map.scripting.util.LuaHelpers.noSuchKey;
import static net.hollowcube.mapmaker.map.scripting.util.LuaHelpers.noSuchMethod;

public class LuaEntity {
    private static final String NAME = "Entity";

    public static void init(@NotNull LuaState state) {
        // Create the metatable for Entity
        state.newMetaTable(NAME);
        state.pushCFunction(LuaEntity::luaIndex, "__index");
        state.setField(-2, "__index");
        state.pushCFunction(LuaEntity::luaNewIndex, "__newindex");
        state.setField(-2, "__newindex");
        state.pushCFunction(LuaEntity::luaNameCall, "__namecall");
        state.setField(-2, "__namecall");
        state.pop(1);
    }

    public static void push(@NotNull LuaState state, @NotNull LuaEntity player) {
        state.newUserData(player);
        state.getMetaTable(NAME);
        state.setMetaTable(-2);
    }

    public static @NotNull LuaEntity checkArg(@NotNull LuaState state, int index) {
        return (LuaEntity) state.checkUserDataArg(index, NAME);
    }

    private final Entity delegate;

    public LuaEntity(@NotNull Entity delegate) {
        this.delegate = delegate;
    }

    public @NotNull Entity delegate() {
        return delegate;
    }

    // Properties

    private int getUuid(@NotNull LuaState state) {
        state.pushString(delegate().getUuid().toString());
        return 1;
    }

    private int getPosition(@NotNull LuaState state) {
        LuaVectorTypeImpl.push(state, delegate().getPosition());
        return 1;
    }

    // Methods

    // Metamethods

    protected static int luaIndex(@NotNull LuaState state, @NotNull String typeName, @NotNull LuaEntity entity, @NotNull String methodName) {
        return switch (methodName) {
            case "Uuid" -> entity.getUuid(state);
            case "Position" -> entity.getPosition(state);
            default -> noSuchKey(state, typeName, methodName);
        };
    }

    protected static int luaNewIndex(@NotNull LuaState state, @NotNull String typeName, @NotNull LuaEntity entity, @NotNull String methodName) {
        return switch (methodName) {
            default -> noSuchKey(state, typeName, methodName);
        };
    }

    protected static int luaNameCall(@NotNull LuaState state, @NotNull String typeName, @NotNull LuaEntity entity, @NotNull String methodName) {
        return switch (methodName) {
            case "Uuid" -> entity.getUuid(state);
            case "Position" -> entity.getPosition(state);
            default -> noSuchMethod(state, typeName, methodName);
        };
    }

    private static int luaIndex(@NotNull LuaState state) {
        final LuaEntity entity = checkArg(state, 1);
        final String key = state.checkStringArg(2);
        return luaIndex(state, NAME, entity, key);
    }

    private static int luaNewIndex(@NotNull LuaState state) {
        final LuaEntity entity = checkArg(state, 1);
        final String key = state.checkStringArg(2);
        state.remove(1); // Remove the userdata from the stack
        state.remove(1); // Remove the key from the stack
        return luaNewIndex(state, NAME, entity, key);
    }

    private static int luaNameCall(@NotNull LuaState state) {
        final LuaEntity entity = checkArg(state, 1);
        state.remove(1); // Remove the player userdata from the stack
        final String methodName = state.nameCallAtom();
        return luaNameCall(state, NAME, entity, methodName);
    }

}
