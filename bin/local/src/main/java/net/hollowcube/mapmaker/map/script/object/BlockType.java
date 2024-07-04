package net.hollowcube.mapmaker.map.script.object;

import net.hollowcube.common.util.BlockUtil;
import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.LuaType;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

public class BlockType {
    public static final String TYPE_NAME = BlockType.class.getName();

    public static void initGlobalLib(@NotNull LuaState state) {
        {
            state.newTable();

            state.newTable(); // Metatable
            state.pushCFunction(BlockType::luaIndexGroup, "__index");
            state.setField(-2, "__index");
            state.pushCFunction(BlockType::luaNameCallGroup, "__namecall");
            state.setField(-2, "__namecall");

            state.setMetaTable(-2);
//            state.setReadOnly(-2, true); //todo
            state.setGlobal("blocks");
        }
        {
            //todo ideally block should be a userdata without destructor or any ref tracking. We should just write
            // the integer into the userdata and be done with it. When the memory gets freed thats good.
            state.newMetaTable(TYPE_NAME);
            state.pushCFunction(BlockType::luaIndex, "__index");
            state.setField(-2, "__index");
            state.pushCFunction(BlockType::luaNewIndex, "__newindex");
            state.setField(-2, "__newindex");
            state.pushCFunction(BlockType::luaNameCall, "__namecall");
            state.setField(-2, "__namecall");
            state.pushCFunction(BlockType::luaCall, "__call");
            state.setField(-2, "__call");
            state.pushCFunction(BlockType::luaToString, "__tostring");
            state.setField(-2, "__tostring");
            state.pushCFunction(BlockType::luaEq, "__eq");
            state.setField(-2, "__eq");
            state.pop(1);
        }
    }

    public static void pushBlock(@NotNull LuaState state, @NotNull Block block) {
        state.newUserData(block.stateId());
        state.getMetaTable(TYPE_NAME);
        state.setMetaTable(-2);
    }

    private static int luaIndexGroup(@NotNull LuaState state) {
        String key = state.checkStringArg(2);

        var block = Block.fromNamespaceId(key.toLowerCase(Locale.ROOT));
        if (block == null) {
            state.argError(2, "No such block: " + key);
            return 0;
        }

        pushBlock(state, block);
        return 1;
    }

    private static int luaNameCallGroup(@NotNull LuaState state) {
        String method = state.nameCallAtom();

        return switch (method) {
            default -> {
                state.error("No such method: " + method);
                yield 0; // Never reached
            }
        };
    }

    private static int luaIndex(@NotNull LuaState state) {
        int ref = (Integer) state.checkUserDataArg(1, TYPE_NAME);
        String key = state.checkStringArg(2);

        var block = Block.fromStateId(ref);
        var prop = block.getProperty(key);
        if (prop == null) {
            state.error("No such key: " + key);
            return 0;
        }

        state.pushString(prop);
        return 1;
    }

    private static int luaNewIndex(@NotNull LuaState state) {
        int ref = (Integer) state.checkUserDataArg(1, TYPE_NAME);
        String key = state.checkStringArg(2);
        String value = state.checkStringArg(3);

        try {
            var block = Block.fromStateId(ref);
            pushBlock(state, block.withProperty(key, value));
            return 1;
        } catch (IllegalArgumentException e) {
            state.error("Invalid property value: " + key + "=" + value);
            return 0;
        }
    }

    private static int luaNameCall(@NotNull LuaState state) {
        int ref = (Integer) state.checkUserDataArg(1, TYPE_NAME);
        String method = state.nameCallAtom();

        return switch (method) {
            default -> {
                state.error("No such method: " + method);
                yield 0; // Never reached
            }
        };
    }

    private static int luaCall(@NotNull LuaState state) {
        int ref = (Integer) state.checkUserDataArg(1, TYPE_NAME);
        state.checkType(2, LuaType.TABLE);

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
            var block = Block.fromStateId(ref);
            pushBlock(state, block.withProperties(newProps));
            return 1;
        } catch (IllegalArgumentException e) {
            state.error(e.getMessage());
            return 0;
        }
    }

    private static int luaToString(@NotNull LuaState state) {
        int ref = (int) state.checkUserDataArg(1, TYPE_NAME);
        Block block = Objects.requireNonNull(Block.fromStateId(ref));
        state.pushString(BlockUtil.toString(block));
        return 1;
    }

    private static int luaEq(@NotNull LuaState state) {
        var left = Block.fromStateId((int) state.checkUserDataArg(1, TYPE_NAME)).id();
        var right = Block.fromStateId((int) state.checkUserDataArg(2, TYPE_NAME)).id();
        state.pushBoolean(left == right);
        return 1;
    }


}
