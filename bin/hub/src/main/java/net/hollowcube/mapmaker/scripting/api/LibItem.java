package net.hollowcube.mapmaker.scripting.api;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.gen.LuaExport;
import net.hollowcube.luau.gen.LuaLibrary;
import net.hollowcube.luau.gen.LuaProperty;
import net.hollowcube.mapmaker.scripting.ScriptContext;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.item.ItemStack;

import java.util.Objects;

@LuaLibrary(name = "@mapmaker/item")
public final class LibItem {

    private static final EquipmentSlot[] SLOTS = EquipmentSlot.values();
    public static final int SLOT_TAG = 1; // todo: slopgen so we dont reuse the same tag

    public static EquipmentSlot checkSlot(LuaState state, int index) {
        if (SLOT_TAG != state.lightUserDataTag(index)) {
            state.argError(index, "Expected Slot");
            return EquipmentSlot.MAIN_HAND; // unreachable
        }
        return SLOTS[(int) state.toLightUserData(index)];
    }

    public static void registerSlotGlobal(LuaState state) {
        state.newTable();
        for (var slot : EquipmentSlot.values()) {
            state.pushLightUserDataTagged(slot.ordinal(), SLOT_TAG);
            state.setField(-2, slot.name());
        }
        state.setReadOnly(-1, true);
        state.setGlobal("Slot");
    }

    public static void pushItem(LuaState state, ItemStack itemStack) {
        if (itemStack.isAir()) {
            state.pushNil();
        } else {
            LibItem$luau.pushItem(state, new Item(itemStack));
        }
    }

    @LuaExport
    public record Item(ItemStack value) {

        @LuaProperty
        public int getId(LuaState state) {
            var itemRegistry = ScriptContext.get(state).world().itemRegistry();
            var itemId = Objects.requireNonNullElse(itemRegistry.getItemId(value), value.material().name());
            state.pushString(itemId);
            return 1;
        }

    }
}
