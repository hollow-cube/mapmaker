package net.hollowcube.datafix.itemStack;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class V4996_01_PotDecorations extends AbstractItemStackUpgradeTest {

    @Test
    void testUnflattenPotDecorations() {
        var result = upgradeC(4903, 4996);

        var decorations = result.getCompound("components").getCompound("minecraft:pot_decorations");
        assertEquals("minecraft:angler_pottery_sherd", decorations.getCompound("back").getString("id"));
        assertEquals("minecraft:brick", decorations.getCompound("left").getString("id"));
        assertEquals("minecraft:blade_pottery_sherd", decorations.getCompound("right").getString("id"));
        assertEquals("minecraft:brick", decorations.getCompound("front").getString("id"));
    }
}
