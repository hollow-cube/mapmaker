package net.hollowcube.datafix.blockEntity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class V4996_01_DecoratedPot extends AbstractBlockEntityUpgradeTest {

    @Test
    void testUnflattenSherds() {
        var result = upgradeC(4903, 4996);

        var sherds = result.getCompound("sherds");
        assertEquals("minecraft:brick", sherds.getCompound("back").getString("id"));
        assertEquals("minecraft:heart_pottery_sherd", sherds.getCompound("left").getString("id"));
        assertEquals("minecraft:brick", sherds.getCompound("right").getString("id"));
        assertEquals("minecraft:skull_pottery_sherd", sherds.getCompound("front").getString("id"));
    }

    @Test
    void testUpgradeContainedItem() {
        var result = upgradeC(4903, 5023);
        assertEquals("minecraft:ocean_monument_map", result.getCompound("item").getString("id"));
    }
}
