package net.hollowcube.mapmaker.scripting.api;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.gen.LuaLibrary;
import net.hollowcube.luau.gen.LuaProperty;
import net.minestom.server.MinecraftServer;

@LuaLibrary(name = "@mapmaker/env")
public final class LibEnv {

    /// The player object for the currently running script.
    ///
    /// Only available for player-bound scripts, otherwise will throw.
    ///
    /// @luaReturn Player.Player
    @LuaProperty
    public static int getPlayer(LuaState state) {
        var p = MinecraftServer.getConnectionManager().getOnlinePlayers().stream().findFirst().orElseThrow();
        LibPlayer.pushPlayer(state, p);
        return 1;
    }

}
