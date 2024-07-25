package net.hollowcube.mapmaker.map.script.api.item;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.LuaType;
import net.hollowcube.luau.annotation.LuaMeta;
import net.hollowcube.luau.annotation.LuaObject;
import net.hollowcube.luau.annotation.LuaTypeImpl;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;

@LuaTypeImpl(type = ItemStack.class, name = "ItemStack")
public final class ItemStackTypeImpl {

    private static final Map<String, BiConsumer<LuaState, ItemStack.Builder>> COMPONENT_HANDLERS = new HashMap<>();

    public static void init(@NotNull LuaState state) {
        ItemStackTypeImpl$Wrapper.initMetatable(state);
        ItemsContainer$Wrapper.initMetatable(state);

        state.newUserData(ItemsContainer.INSTANCE);
        state.getMetaTable(ItemsContainer$Wrapper.TYPE_NAME);
        state.setMetaTable(-2);
        state.setGlobal("items");
    }

    public static void pushLuaValue(@NotNull LuaState state, @NotNull ItemStack itemStack) {
        state.newUserData(itemStack);
        state.getMetaTable(ItemStackTypeImpl$Wrapper.TYPE_NAME);
        state.setMetaTable(-2);
    }

    public static @NotNull ItemStack checkLuaArg(@NotNull LuaState state, int index) {
        return (ItemStack) state.checkUserDataArg(index, ItemStackTypeImpl$Wrapper.TYPE_NAME);
    }

    @LuaMeta(LuaMeta.Type.CALL)
    static int luaCall(@NotNull LuaState state) {
        var itemStack = checkLuaArg(state, 1).builder();

        state.pushNil();
        while (state.next(2)) {
            // Key is at index -2, value is at index -1
            String key = state.toString(-2);
            var handler = COMPONENT_HANDLERS.get(key);
            if (handler == null) {
                state.argError(2, "Unknown item component: " + key);
                return 0;
            }
            handler.accept(state, itemStack);

            // Remove the value, keep the key for the next iteration
            state.pop(1);
        }

        pushLuaValue(state, itemStack.build());
        return 1;
    }

    @LuaMeta(LuaMeta.Type.TOSTRING)
    static int luaToString(@NotNull LuaState state) {
        var itemStack = checkLuaArg(state, 1);
        state.pushString(itemStack.toString());
        return 1;
    }

    @LuaMeta(LuaMeta.Type.EQ)
    static int luaEqBlock(@NotNull LuaState state) {
        var left = checkLuaArg(state, 1);
        var right = checkLuaArg(state, 2);
        state.pushBoolean(left.isSimilar(right));
        return 1;
    }

    /**
     * The global used to fetch block instances, eg `blocks.STONE`.
     */
    @LuaObject
    public static class ItemsContainer {
        public static final ItemsContainer INSTANCE = new ItemsContainer();

        private ItemsContainer() {
        }

        @LuaMeta(LuaMeta.Type.INDEX)
        public @NotNull ItemStack get(@NotNull String name) {
            var material = Material.fromNamespaceId(name.toLowerCase(Locale.ROOT));
            if (material == null)
                throw new IllegalArgumentException("No such item: " + name);
            return ItemStack.of(material);
        }

    }

    static {
        COMPONENT_HANDLERS.put("CustomModelData", (state, builder) -> {
            state.checkType(-1, LuaType.NUMBER);
            builder.customModelData(state.checkIntegerArg(-1));
        });
    }

}
