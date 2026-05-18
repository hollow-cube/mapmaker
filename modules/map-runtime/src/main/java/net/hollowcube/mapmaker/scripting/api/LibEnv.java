package net.hollowcube.mapmaker.scripting.api;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.gen.LuaLibrary;
import net.hollowcube.luau.gen.LuaProperty;
import net.hollowcube.mapmaker.scripting.LegacyScriptContext;

/// Information about the script's environment.
@LuaLibrary(name = "@mapmaker/env")
public final class LibEnv {

    /// The player this script is attached to. Only valid in player-bound scripts; reading
    /// from a world-bound script raises an error.
    ///
    /// @luaReturn @mapmaker/player.Player
    @LuaProperty
    public static int getPlayer(LuaState state) {
        var context = LegacyScriptContext.get(state);
        if (!(context instanceof LegacyScriptContext.Player playerContext))
            throw state.error("environment player is only available in player-bound scripts");

        LibPlayer.pushPlayer(state, playerContext.player());
        return 1;
    }

    /// The world this script is attached to. Only valid in world-bound scripts.
    ///
    /// @luaReturn @mapmaker/world.World
    @LuaProperty
    public static int getWorld(LuaState state) {
        var context = LegacyScriptContext.get(state);
        if (!(context instanceof LegacyScriptContext.World worldContext))
            throw state.error("environment player is only available in player-bound scripts");

//        LibWorld.pushWorld(state, worldContext.world());
        throw new UnsupportedOperationException("todo");
    }

}
