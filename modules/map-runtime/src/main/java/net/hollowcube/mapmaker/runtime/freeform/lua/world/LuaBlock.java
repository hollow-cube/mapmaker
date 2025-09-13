package net.hollowcube.mapmaker.runtime.freeform.lua.world;

import net.hollowcube.common.util.BlockUtil;
import net.hollowcube.common.util.StringUtil;
import net.hollowcube.luau.LuaState;
import net.minestom.server.instance.block.Block;

import java.util.HashMap;

public class LuaBlock {
    public static final String NAME = "Block";

    public static void init(LuaState state) {
        // Create the metatable for Minestom Block
        state.newMetaTable(NAME);
        state.pushCFunction(LuaBlock::luaToString, "__tostring");
        state.setField(-2, "__tostring");
        state.pushCFunction(LuaBlock::luaCall, "__call");
        state.setField(-2, "__call");
        state.pushCFunction(LuaBlock::luaEq, "__eq");
        state.setField(-2, "__eq");
        state.pop(1);

        // Global table of all blocks
        // todo this should probably just be an index metamethod, theres no need to prealloc this.
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

    public static void push(LuaState state, Block block) {
        state.newUserData(block);
        state.getMetaTable(NAME);
        state.setMetaTable(-2);
    }

    public static Block checkArg(LuaState state, int index) {
        return (Block) state.checkUserDataArg(index, NAME);
    }

    private static int luaToString(LuaState state) {
        var block = checkArg(state, 1);
        state.pushString(BlockUtil.toString(block));
        return 1;
    }

    private static int luaCall(LuaState state) {
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

    private static int luaEq(LuaState state) {
        var block1 = checkArg(state, 1);
        var block2 = checkArg(state, 2);
        state.pushBoolean(block1.stateId() == block2.stateId());
        return 1;
    }
}

