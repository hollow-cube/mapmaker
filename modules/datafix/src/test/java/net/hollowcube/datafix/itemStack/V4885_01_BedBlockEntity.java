package net.hollowcube.datafix.itemStack;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class V4885_01_BedBlockEntity extends AbstractItemStackUpgradeTest {

    @Test
    void testRemoveBedBlockEntityData() {
        var result = upgradeC(4763, 4888);

        var components = result.getCompound("components");
        assertFalse(components.keySet().contains("minecraft:block_entity_data"),
            "bed block_entity_data component should be removed");
        assertEquals(5, components.getInt("minecraft:damage"));
    }
}
