package net.hollowcube.mapmaker.map.script.type;

import net.hollowcube.common.util.BlockUtil;
import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.annotation.LuaMeta;
import net.hollowcube.luau.annotation.LuaObject;
import net.hollowcube.luau.annotation.LuaTypeImpl;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

@LuaTypeImpl(type = Block.class, name = "BlockType")
public final class BlockTypeImpl {

    public static void pushLuaValue(@NotNull LuaState state, @NotNull Block block) {
        state.newUserDataInt(block.stateId());
        state.getMetaTable(BlockTypeImpl$Wrapper.TYPE_NAME);
        state.setMetaTable(-2);
    }

    public static @NotNull Block checkLuaArg(@NotNull LuaState state, int index) {
        int stateId = state.checkUserDataIntArg(index, BlockTypeImpl$Wrapper.TYPE_NAME);
        return Objects.requireNonNull(Block.fromStateId(stateId));
    }

    @LuaMeta(LuaMeta.Type.INDEX)
    static @Nullable String luaIndex(@NotNull Block block, @NotNull String key) {
        return block.getProperty(key); // May return null, which is converted to lua nil
    }

    @LuaMeta(LuaMeta.Type.NEWINDEX)
    static @NotNull Block luaNewIndex(@NotNull Block block, @NotNull String key, @NotNull String value) {
        try {
            return block.withProperty(key, value);
        } catch (IllegalArgumentException e) {
            return block;
        }
    }

    @LuaMeta(LuaMeta.Type.CALL)
    static int luaCall(@NotNull LuaState state) {
        var block = checkLuaArg(state, 1);

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
            pushLuaValue(state, block.withProperties(newProps));
            return 1;
        } catch (IllegalArgumentException e) {
            state.error(e.getMessage());
            return 0;
        }
    }

//    @LuaMeta(LuaMeta.Type.CALL)
//    static @NotNull Block luaCall(@NotNull Block block, @NotNull LuaTableView table) {
//        var newProps = new HashMap<String, String>();
//        for (var iter = table.iterator(); iter.hasNext(); ) {
//            newProps.put(iter.getStringKey(), iter.getStringValue());
//        }
//
//        try {
//            return block.withProperties(newProps);
//        } catch (IllegalArgumentException e) {
//            return block;
//        }
//    }

    @LuaMeta(LuaMeta.Type.TOSTRING)
    static @NotNull String luaToString(@NotNull Block block) {
        return BlockUtil.toString(block);
    }

    @LuaMeta(LuaMeta.Type.EQ)
    static boolean luaEqBlock(@NotNull Block left, @NotNull Block right) {
        return left.id() == right.id();
    }

    private BlockTypeImpl() {
    }

    /**
     * The global used to fetch block instances, eg `blocks.STONE`.
     */
    @LuaObject
    public static class BlocksContainer {
        public static final BlocksContainer INSTANCE = new BlocksContainer();

        private BlocksContainer() {
        }

        @LuaMeta(LuaMeta.Type.INDEX)
        public @NotNull Block get(@NotNull String name) {
            var block = Block.fromNamespaceId(name.toLowerCase(Locale.ROOT));
            if (block == null)
                throw new IllegalArgumentException("No such block: " + name);
            return block;
        }

    }

}
