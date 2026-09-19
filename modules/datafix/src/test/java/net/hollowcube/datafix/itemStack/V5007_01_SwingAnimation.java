package net.hollowcube.datafix.itemStack;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class V5007_01_SwingAnimation extends AbstractItemStackUpgradeTest {

    @Test
    void testSplitSwingAnimation() {
        var result = upgradeC(4903, 5007);

        var components = result.getCompound("components");
        assertFalse(components.keySet().contains("minecraft:swing_animation"));
        for (var key : new String[]{"minecraft:attack_animation", "minecraft:interact_animation"}) {
            var animation = components.getCompound(key);
            assertEquals("stab", animation.getString("type"));
            assertEquals(8, animation.getInt("duration"));
        }
    }
}
