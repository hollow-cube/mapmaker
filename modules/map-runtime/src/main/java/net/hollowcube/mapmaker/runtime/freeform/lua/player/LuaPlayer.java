package net.hollowcube.mapmaker.runtime.freeform.lua.player;

import com.google.gson.JsonObject;
import net.hollowcube.luau.LuaState;
import net.hollowcube.mapmaker.runtime.freeform.lua.LuaEventSource;
import net.hollowcube.mapmaker.runtime.freeform.lua.math.LuaVectorTypeImpl;
import net.hollowcube.mapmaker.runtime.freeform.lua.world.LuaBlock;
import net.hollowcube.mapmaker.runtime.freeform.script.LuaHelpers;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerBlockInteractEvent;

import static net.hollowcube.mapmaker.runtime.freeform.script.LuaHelpers.noSuchKey;
import static net.hollowcube.mapmaker.runtime.freeform.script.LuaHelpers.noSuchMethod;

public class LuaPlayer {
    private static final String NAME = "Player";

    public static void init(LuaState state) {
        state.newMetaTable(NAME);
        state.pushCFunction(LuaPlayer::luaIndex, "__index");
        state.setField(-2, "__index");
        state.pushCFunction(LuaPlayer::luaNewIndex, "__newindex");
        state.setField(-2, "__newindex");
        state.pushCFunction(LuaPlayer::luaNameCall, "__namecall");
        state.setField(-2, "__namecall");
        state.pop(1);
    }

    public static void push(LuaState state, LuaPlayer entity) {
        state.newUserData(entity);
        state.getMetaTable(NAME);
        state.setMetaTable(-2);
    }

    public static LuaPlayer checkArg(LuaState state, int index) {
        return (LuaPlayer) state.checkUserDataArg(index, NAME);
    }

    private final Player player;
    private final int saveDataRef;

    public LuaPlayer(LuaState state, Player player, JsonObject saveData) {
        this.player = player;

        LuaHelpers.pushJsonElement(state, saveData);
        this.saveDataRef = state.ref(-1); // todo dont leak this :)
        state.pop(1);
    }

    // Properties

    private int getUuid(LuaState state) {
        state.pushString(player.getUuid().toString());
        return 1;
    }

    private int getSaveData(LuaState state) {
        state.getref(saveDataRef);
        return 1;
    }

    private int getOnBlockInteract(LuaState state) {
        LuaEventSource.push(state, new LuaEventSource<>(
                player.eventNode(),
                PlayerBlockInteractEvent.class,
                (eventState, event) -> {
                    LuaVectorTypeImpl.push(eventState, event.getBlockPosition());
                    LuaBlock.push(eventState, event.getBlock());
                    return 2;
                }
        ));
        return 1;
    }

    // Methods

    // Metamethods

    private static int luaIndex(LuaState state) {
        final LuaPlayer self = checkArg(state, 1);
        final String key = state.checkStringArg(2);
        return switch (key) {
            case "Uuid" -> self.getUuid(state);
            case "SaveData" -> self.getSaveData(state);
            case "OnBlockInteract" -> self.getOnBlockInteract(state);
            default -> noSuchKey(state, NAME, key);
        };
    }

    private static int luaNewIndex(LuaState state) {
        final LuaPlayer self = checkArg(state, 1);
        final String key = state.checkStringArg(2);
        state.remove(1); // Remove the userdata from the stack
        state.remove(1); // Remove the key from the stack
        return switch (key) {
            default -> noSuchKey(state, NAME, key);
        };
    }

    private static int luaNameCall(LuaState state) {
        final LuaPlayer self = checkArg(state, 1);
        state.remove(1); // Remove the world userdata from the stack (so implementations can pretend they have no self)
        final String methodName = state.nameCallAtom();
        return switch (methodName) {
            default -> noSuchMethod(state, NAME, methodName);
        };
    }
}
