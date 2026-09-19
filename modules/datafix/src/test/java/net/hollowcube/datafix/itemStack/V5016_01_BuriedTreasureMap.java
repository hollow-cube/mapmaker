package net.hollowcube.datafix.itemStack;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class V5016_01_BuriedTreasureMap extends AbstractItemStackUpgradeTest {

    @Test
    void testRenameUnfilledBuriedTreasureMap() {
        var result = upgradeC(4903, 5016);

        var itemName = result.getCompound("components").getCompound("minecraft:item_name");
        assertEquals("item.minecraft.buried_treasure_map", itemName.getString("translate"));
    }
}
