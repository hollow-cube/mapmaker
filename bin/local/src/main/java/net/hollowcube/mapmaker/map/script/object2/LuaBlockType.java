package net.hollowcube.mapmaker.map.script.object2;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.annotation.LuaTypeImpl;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

@LuaTypeImpl(Block.class)
public class LuaBlockType {

    public static void pushValue(@NotNull LuaState state, @NotNull Block block) {
//        state.pushString(block.name()); //todo
    }

    public static Block checkArg(@NotNull LuaState state, int index) {
        throw new UnsupportedOperationException("todo");
    }

}
