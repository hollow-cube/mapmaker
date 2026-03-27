package net.hollowcube.mapmaker.panels;

import it.unimi.dsi.fastutil.ints.IntIntPair;
import net.minestom.server.inventory.InventoryType;

import java.util.EnumMap;

class SpecialInventoryHosts {

    private static final IntIntPair[] EMPTY_OVERRIDES = new IntIntPair[0];
    private static final EnumMap<InventoryType, IntIntPair[]> SLOT_POSITION_OVERRIDES = new EnumMap<>(InventoryType.class);

    static {
        SLOT_POSITION_OVERRIDES.put(InventoryType.CARTOGRAPHY, new IntIntPair[]{
            IntIntPair.of(6, 3),
            IntIntPair.of(6, 40),
            IntIntPair.of(136, 27)
        });
    }

    /**
     * Certain inventory types have special slot position overrides that differ from the standard grid layout.
     *
     * @return An array of slots and their positions that override the default layout.
     * If the slot index is larger than the size of the array then it's not overridden.
     */
    static IntIntPair[] getSlotPositionOverrides(InventoryType type) {
        return SLOT_POSITION_OVERRIDES.getOrDefault(type, EMPTY_OVERRIDES);
    }

    /**
     * We have special handling for inventories that don't exactly match the expected grid/column layout we use
     * for inventories. For example an anvil has its 3 special slots but we pretend it has 9 slots for the sake
     * of the layout system. Slots 3-8 are skipped in that case.
     *
     * @return The size of the inventory as interpreted by the layout system.
     */
    static int getContainerSize(InventoryType type) {
        return switch (type) {
            case CHEST_1_ROW, CHEST_2_ROW, CHEST_3_ROW,
                 CHEST_4_ROW, CHEST_5_ROW, CHEST_6_ROW -> type.getSize();
            case ANVIL, CARTOGRAPHY -> 9;
            default -> throw new IllegalStateException("Unsupported inventory type: " + type);
        };
    }
}
