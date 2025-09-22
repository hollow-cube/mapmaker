package net.hollowcube.mapmaker.runtime.freeform.lua.player;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.annotation.LuaProperty;
import net.hollowcube.luau.annotation.LuaType;
import net.hollowcube.mapmaker.runtime.freeform.lua.base.LuaTextImpl;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.scoreboard.Sidebar;

@LuaType
public class LuaSidebar implements LuaSidebar$luau {

    public static void push(LuaState state, LuaSidebar value) {
        state.newUserData(value);
        state.getMetaTable(TYPE_NAME);
        state.setMetaTable(-2);
    }

    public static LuaSidebar checkArg(LuaState state, int index) {
        return (LuaSidebar) state.checkUserDataArg(index, TYPE_NAME);
    }

    private final Sidebar sidebar;
    private final Player player;

    // Not stored on Sidebar and we want to be able to return it, so stored here.
    private Component title = Component.empty();

    public LuaSidebar(Player player) {
        this.sidebar = new Sidebar(title);
        this.player = player;
    }

    @LuaProperty
    public int getEnabled(LuaState state) {
        state.pushBoolean(sidebar.isViewer(player));
        return 1;
    }

    @LuaProperty
    public int setEnabled(LuaState state) {
        boolean newValue = state.checkBooleanArg(1);
        if (newValue) sidebar.addViewer(player);
        else sidebar.removeViewer(player);
        return 1;
    }

    @LuaProperty
    public int getTitle(LuaState state) {
        LuaTextImpl.push(state, title);
        return 1;
    }

    @LuaProperty
    public int setTitle(LuaState state) {
        title = LuaTextImpl.checkAnyTextArg(state, 1);
        sidebar.setTitle(title);
        return 1;
    }

}
