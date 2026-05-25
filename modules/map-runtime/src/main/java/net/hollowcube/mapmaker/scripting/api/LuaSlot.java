package net.hollowcube.mapmaker.scripting.api;

import net.hollowcube.luau.LuaState;
import net.hollowcube.scripting.gen.LuaEnum;
import net.hollowcube.scripting.gen.LuaLibrary;
import net.minestom.server.entity.EquipmentSlot;

@LuaEnum(name = "Slot", scope = LuaLibrary.Scope.GLOBAL)
public enum LuaSlot {
    MAIN_HAND(EquipmentSlot.MAIN_HAND),
    OFF_HAND(EquipmentSlot.OFF_HAND),
    BOOTS(EquipmentSlot.BOOTS),
    LEGGINGS(EquipmentSlot.LEGGINGS),
    CHESTPLATE(EquipmentSlot.CHESTPLATE),
    HELMET(EquipmentSlot.HELMET),
    BODY(EquipmentSlot.BODY),
    SADDLE(EquipmentSlot.SADDLE);

    private static final LuaSlot[] BY_EQUIPMENT_SLOT = new LuaSlot[EquipmentSlot.values().length];

    static {
        BY_EQUIPMENT_SLOT[EquipmentSlot.MAIN_HAND.ordinal()] = MAIN_HAND;
        BY_EQUIPMENT_SLOT[EquipmentSlot.OFF_HAND.ordinal()] = OFF_HAND;
        BY_EQUIPMENT_SLOT[EquipmentSlot.BOOTS.ordinal()] = BOOTS;
        BY_EQUIPMENT_SLOT[EquipmentSlot.LEGGINGS.ordinal()] = LEGGINGS;
        BY_EQUIPMENT_SLOT[EquipmentSlot.CHESTPLATE.ordinal()] = CHESTPLATE;
        BY_EQUIPMENT_SLOT[EquipmentSlot.HELMET.ordinal()] = HELMET;
        BY_EQUIPMENT_SLOT[EquipmentSlot.BODY.ordinal()] = BODY;
        BY_EQUIPMENT_SLOT[EquipmentSlot.SADDLE.ordinal()] = SADDLE;
    }

    private final EquipmentSlot slot;

    LuaSlot(EquipmentSlot slot) {
        this.slot = slot;
    }

    public static EquipmentSlot checkArg(LuaState state, int index) {
        return LuaSlot$luau.checkLuaSlotArg(state, index).slot;
    }

    public static void push(LuaState state, EquipmentSlot slot) {
        LuaSlot$luau.pushLuaSlot(state, BY_EQUIPMENT_SLOT[slot.ordinal()]);
    }
}
