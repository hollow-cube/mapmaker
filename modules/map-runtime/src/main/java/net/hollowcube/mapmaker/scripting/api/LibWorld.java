package net.hollowcube.mapmaker.scripting.api;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.gen.LuaLibrary;
import net.hollowcube.luau.gen.LuaMethod;
import net.hollowcube.mapmaker.scripting.LegacyScriptContext;

/// Read and write the world this script is running in. For per-player effects, use
/// `player.world` instead.
@LuaLibrary(name = "@mapmaker/world")
public final class LibWorld {

    /// Returns the block at the given position. Currently returns the block's id as a
    /// string (e.g. `"minecraft:stone"`).
    ///
    /// @luaParam position vector
    /// @luaReturn string
    @LuaMethod
    public static int getBlock(LuaState state) {
        var world = LegacyScriptContext.get(state).world();
        var pos = LuaVector.check(state, 1);

        var block = world.instance().getBlock(pos);
        // TODO: this should return a Block not a string
        state.pushString(block.name());
        return 1;
    }


}
