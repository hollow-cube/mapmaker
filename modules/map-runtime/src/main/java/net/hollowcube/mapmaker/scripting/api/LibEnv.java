package net.hollowcube.mapmaker.scripting.api;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.gen.LuaLibrary;
import net.hollowcube.luau.gen.LuaProperty;
import net.hollowcube.mapmaker.scripting.ScriptContext;

@LuaLibrary(name = "@mapmaker/env")
public final class LibEnv {

    /// The player object for the currently running script.
    ///
    /// Only available for player-bound scripts, otherwise will throw.
    ///
    /// @luaReturn Player.Player
    @LuaProperty
    public static int getPlayer(LuaState state) {
        var context = ScriptContext.get(state);
        if (!(context instanceof ScriptContext.Player playerContext))
            throw state.error("environment player is only available in player-bound scripts");

        LibPlayer.pushPlayer(state, playerContext.player());
        return 1;
    }

    @LuaProperty
    public static int getWorld(LuaState state) {
        var context = ScriptContext.get(state);
        if (!(context instanceof ScriptContext.World worldContext))
            throw state.error("environment player is only available in player-bound scripts");

//        LibWorld.pushWorld(state, worldContext.world());
        throw new UnsupportedOperationException("todo");
    }

}
