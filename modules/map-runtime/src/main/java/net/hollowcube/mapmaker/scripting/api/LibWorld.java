package net.hollowcube.mapmaker.scripting.api;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.gen.LuaLibrary;
import net.hollowcube.luau.gen.LuaMethod;
import net.hollowcube.mapmaker.scripting.ScriptContext;

@LuaLibrary(name = "@mapmaker/world")
public final class LibWorld {

    @LuaMethod
    public static int getBlock(LuaState state) {
        var world = ScriptContext.get(state).world();
        var pos = LuaVector.check(state, 1);

        var block = world.instance().getBlock(pos);
        // TODO: this should return a Block not a string
        state.pushString(block.name());
        return 1;
    }


}
