package net.hollowcube.mapmaker.runtime.freeform.lua.player;

import com.google.gson.JsonObject;
import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.annotation.LuaProperty;
import net.hollowcube.luau.annotation.LuaType;
import net.hollowcube.mapmaker.runtime.freeform.lua.LuaEventSource;
import net.hollowcube.mapmaker.runtime.freeform.lua.base.LuaTextImpl;
import net.hollowcube.mapmaker.runtime.freeform.lua.math.LuaVectorTypeImpl;
import net.hollowcube.mapmaker.runtime.freeform.lua.world.LuaBlockImpl;
import net.hollowcube.mapmaker.runtime.freeform.script.LuaHelpers;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import org.jetbrains.annotations.Nullable;

@LuaType
public class LuaPlayer implements LuaPlayer$luau {

    public static void push(LuaState state, LuaPlayer entity) {
        state.newUserData(entity);
        state.getMetaTable(TYPE_NAME);
        state.setMetaTable(-2);
    }

    public static LuaPlayer checkArg(LuaState state, int index) {
        return (LuaPlayer) state.checkUserDataArg(index, TYPE_NAME);
    }

    private final Player player;
    private final int saveDataRef;

    private @Nullable LuaSidebar sidebar; // Lazy

    public LuaPlayer(LuaState state, Player player, JsonObject saveData) {
        this.player = player;

        LuaHelpers.pushJsonElement(state, saveData);
        this.saveDataRef = state.ref(-1); // todo dont leak this :)
        state.pop(1);
    }

    @LuaProperty
    public int getUuid(LuaState state) {
        state.pushString(player.getUuid().toString());
        return 1;
    }

    @LuaProperty
    public int getName(LuaState state) {
        state.pushString(player.getUsername());
        return 1;
    }

    //region Communication

    @LuaProperty
    public int getSidebar(LuaState state) {
        if (sidebar == null) sidebar = new LuaSidebar(player);
        LuaSidebar.push(state, sidebar);
        return 1;
    }

    public int sendMessage(LuaState state) {
        var message = LuaTextImpl.checkAnyTextArg(state, 1);
        player.sendMessage(message);
        return 0;
    }

    //endregion

    //region Persistence

    @LuaProperty
    public int getSaveData(LuaState state) {
        state.getref(saveDataRef);
        return 1;
    }

    //endregion Persistence

    //region Events

    @LuaProperty
    public int getOnBlockInteract(LuaState state) {
        LuaEventSource.push(state, new LuaEventSource<>(
                player.eventNode(),
                PlayerBlockInteractEvent.class,
                (eventState, event) -> {
                    LuaVectorTypeImpl.push(eventState, event.getBlockPosition());
                    LuaBlockImpl.push(eventState, event.getBlock());
                    return 2;
                }
        ));
        return 1;
    }

    //endregion

}
