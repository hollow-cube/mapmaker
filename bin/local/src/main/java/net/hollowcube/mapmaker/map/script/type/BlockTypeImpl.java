package net.hollowcube.mapmaker.map.script.type;

import net.hollowcube.common.util.BlockUtil;
import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.annotation.LuaMeta;
import net.hollowcube.luau.annotation.LuaObject;
import net.hollowcube.luau.annotation.LuaTypeImpl;
import net.hollowcube.luau.type.LuaTableView;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

@LuaTypeImpl(Block.class)
public final class BlockTypeImpl {

    public static void pushValue(@NotNull LuaState state, @NotNull Block block) {
        state.newUserDataInt(block.stateId());
        state.getMetaTable(BlockTypeImpl$Wrapper.TYPE_NAME);
        state.setMetaTable(-2);
    }

    public static @NotNull Block checkArg(@NotNull LuaState state, int index) {
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
    static @NotNull Block luaCall(@NotNull Block block, @NotNull LuaTableView table) {
        var newProps = new HashMap<String, String>();
        for (var iter = table.iterator(); iter.hasNext(); ) {
            newProps.put(iter.getStringKey(), iter.getStringValue());
        }

        try {
            return block.withProperties(newProps);
        } catch (IllegalArgumentException e) {
            return block;
        }
    }

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
