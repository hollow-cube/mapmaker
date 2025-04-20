package net.hollowcube.datafix.itemStack;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class V99_01_DiamondHelmet extends AbstractItemStackUpgradeTest {
    private static final int CURRENT = 4314;

    @Test
    void idUpgrade() {
        var result = upgradeC(0, CURRENT);
        assertEquals("minecraft:diamond_helmet", result.getString("id"));
    }

    @Test
    void checkExpectedFields() {
        var result = upgradeC(0, CURRENT);
        assertEquals(Set.of("id", "count", "components"), result.keySet());
        assertEquals(Set.of("minecraft:enchantments"), result.getCompound("components").keySet());
    }

    @Test
    void updatedEnchantmentsComponent() {
        var result = upgradeC(0, CURRENT);
        var enchantments = result.getCompound("components").getCompound("minecraft:enchantments");
        assertEquals(2, enchantments.size());
        assertEquals(1, enchantments.getInt("minecraft:projectile_protection"));
        assertEquals(1, enchantments.getInt("minecraft:aqua_affinity"));
    }

}
