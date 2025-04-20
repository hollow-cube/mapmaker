package net.hollowcube.datafix.blockEntity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class V99_05_Furnace extends AbstractBlockEntityUpgradeTest {
    private static final int CURRENT = 4314;

    @Test
    void upgradeId() {
        var result = upgradeC(0, CURRENT);
        assertEquals("minecraft:furnace", result.getString("id"));
    }

    @Test
    void renamedFields() {
        var result = upgradeC(0, CURRENT);
        assertEquals((short) 15853, result.getShort("lit_total_time"));
        assertEquals((short) 148, result.getShort("cooking_time_spent"));
        assertEquals((short) 200, result.getShort("cooking_total_time"));
    }

    @Test
    void itemUpgrades() {
        var result = upgradeC(0, CURRENT);
        var items = result.getList("Items");
        assertEquals(2, items.size());

        var slot0 = items.getCompound(0);
        assertEquals((byte) 0, slot0.getByte("Slot"));
        assertEquals(64, slot0.getInt("count"));
        assertEquals("minecraft:salmon", slot0.getString("id"));
        assertNull(slot0.get("components")); // empty so omit

        var slot1 = items.getCompound(1);
        assertEquals((byte) 1, slot1.getByte("Slot"));
        assertEquals(63, slot1.getInt("count"));
        assertEquals("minecraft:coal_block", slot1.getString("id"));
    }

}
