package net.hollowcube.mapmaker.scripting.api;

import net.hollowcube.luau.LuaState;
import net.hollowcube.mapmaker.scripting.ScriptContext;
import net.hollowcube.scripting.gen.LuaExport;
import net.hollowcube.scripting.gen.LuaLibrary;
import net.hollowcube.scripting.gen.LuaMethod;
import net.hollowcube.scripting.gen.LuaProperty;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

import java.util.Objects;

/// Items and item stacks.
@LuaLibrary(name = "@mapmaker/item")
public final class LibItem {

    public static void pushItem(LuaState state, ItemStack itemStack) {
        if (itemStack.isAir()) {
            state.pushNil();
        } else {
            LibItem$luau.pushItem(state, new Item(itemStack));
        }
    }

    public static ItemStack checkItemArg(LuaState state, int argIndex) {
        return LibItem$luau.checkItemArg(state, argIndex).value;
    }

    /// **Deprecated.** Creates an `Item` from a vanilla material key.
    ///
    /// @luaParam material string
    /// @luaReturn @mapmaker/item.Item
    @LuaMethod
    public static int deprecated_makeItemFromMaterial(LuaState state) {
        var name = state.checkString(1);
        var material = Material.fromKey(name);
        if (material == null) throw state.error("Invalid material: " + name);

        pushItem(state, ItemStack.of(material));
        return 1;
    }

    /// A stack of items. Empty stacks are returned as `nil` rather than an `Item`.
    @LuaExport
    public record Item(ItemStack value) {

        /// The item's id.
        ///
        /// @luaReturn string
        @LuaProperty
        public int getId(LuaState state) {
            var itemRegistry = ScriptContext.get(state).runtime().world().itemRegistry();
            var itemId = Objects.requireNonNullElse(itemRegistry.getItemId(value), value.material().name());
            state.pushString(itemId);
            return 1;
        }

    }
}
