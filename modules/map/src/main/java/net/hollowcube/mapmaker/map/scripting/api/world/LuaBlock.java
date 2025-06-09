package net.hollowcube.mapmaker.map.scripting.api.world;

import net.hollowcube.common.util.BlockUtil;
import net.hollowcube.common.util.StringUtil;
import net.hollowcube.luau.LuaState;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class LuaBlock {
    public static final String NAME = "Block";

    public static void init(@NotNull LuaState state) {
        // Create the metatable for Minestom Block
        state.newMetaTable(NAME);
        state.pushCFunction(LuaBlock::luaToString, "__tostring");
        state.setField(-2, "__tostring");
        state.pushCFunction(LuaBlock::luaCall, "__call");
        state.setField(-2, "__call");
        state.pop(1);

        // Global table of all blocks
        state.newTable();
        for (var block : Block.values()) {
            var friendlyName = StringUtil.snakeToPascal(
                    block.key().value().replace("/", "_"));

            push(state, block);
            state.setField(-2, friendlyName);
        }
        state.setReadOnly(-1, true);
        state.setGlobal("Block");
    }

    public static void push(@NotNull LuaState state, @NotNull Block block) {
        state.newUserData(block);
        state.getMetaTable(NAME);
        state.setMetaTable(-2);
    }

    public static @NotNull Block checkArg(@NotNull LuaState state, int index) {
        return (Block) state.checkUserDataArg(index, NAME);
    }

    private static int luaToString(@NotNull LuaState state) {
        var block = checkArg(state, 1);
        state.pushString(BlockUtil.toString(block));
        return 1;
    }

    private static int luaCall(@NotNull LuaState state) {
        var block = checkArg(state, 1);
        var newProps = new HashMap<String, String>();
        state.pushNil();
        while (state.next(2)) {
            // Key is at index -2, value is at index -1
            String key = state.toString(-2);
            String value = state.toString(-1);
            newProps.put(key, value);

            // Remove the value, keep the key for the next iteration
            state.pop(1);
        }

        try {
            push(state, block.withProperties(newProps));
            return 1;
        } catch (IllegalArgumentException e) {
            state.error(e.getMessage());
            return 0;
        }
    }
}
