package net.hollowcube.mapmaker.runtime.freeform.lua.world;

import net.hollowcube.common.util.BlockUtil;
import net.hollowcube.common.util.StringUtil;
import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.annotation.LuaMeta;
import net.hollowcube.luau.annotation.LuaStatic;
import net.hollowcube.luau.annotation.LuaType;
import net.hollowcube.luau.annotation.MetaType;
import net.kyori.adventure.key.Key;
import net.minestom.server.instance.block.Block;

import java.util.HashMap;

@LuaType(implFor = Block.class, name = "Block")
public class LuaBlockImpl implements LuaBlockImpl$luau {

    public static void push(LuaState state, Block block) {
        state.newUserData(block);
        state.getMetaTable(TYPE_NAME);
        state.setMetaTable(-2);
    }

    public static Block checkArg(LuaState state, int index) {
        return (Block) state.checkUserDataArg(index, TYPE_NAME);
    }

    //region Static Methods

    @LuaStatic
    @LuaMeta(MetaType.INDEX)
    public static int luaStaticIndex(LuaState state) {
        var blockName = state.checkStringArg(1);
        var blockId = StringUtil.pascalToSnake(blockName);
        if (!Key.parseableValue(blockId)) {
            state.argError(1, "Invalid block name");
            return 0;
        }

        var block = Block.fromKey(blockName);
        if (block == null) {
            state.argError(1, "Invalid block name");
            return 0;
        }

        push(state, block);
        return 1;
    }

    //endregion

    //region Meta Methods

    @LuaMeta(MetaType.CALL)
    public static int luaCall(LuaState state) {
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

    @LuaMeta(MetaType.TOSTRING)
    public static int luaToString(LuaState state) {
        var block = checkArg(state, 1);
        state.pushString(BlockUtil.toString(block));
        return 1;
    }

    @LuaMeta(MetaType.EQ)
    public static int luaEq(LuaState state) {
        var block1 = checkArg(state, 1);
        var block2 = checkArg(state, 2);
        state.pushBoolean(block1.stateId() == block2.stateId());
        return 1;
    }

    //endregion
}

