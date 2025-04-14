package net.hollowcube.datafix;

import net.hollowcube.datafix.util.Value;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BlockNameUpgradeTest extends AbstractDataFixTest {
    private static final int SHORT_GRASS_VERSION = 3692;

    @Test
    void testShortGrassFullUpgrade() {
        var actual = upgradeFull(DataTypes.BLOCK_NAME, Value.wrap("minecraft:grass"));
        assertEquals("minecraft:short_grass", actual.value());
    }

    @Test
    void testShortGrassUpgradeToExact() { // Should still be upgraded we got to the exact version
        var actual = upgrade(DataTypes.BLOCK_NAME, Value.wrap("minecraft:grass"), 0, SHORT_GRASS_VERSION);
        assertEquals("minecraft:short_grass", actual.value());
    }

    @Test
    void testShortGrassUpgradeToOneBefore() { // Should NOT be upgraded
        var actual = upgrade(DataTypes.BLOCK_NAME, Value.wrap("minecraft:grass"), 0, SHORT_GRASS_VERSION - 1);
        assertEquals("minecraft:grass", actual.value());
    }

    @Test
    void testShortGrassUpgradeAfterToAfter() { // Should NOT be upgraded since we already claim to be past the fix version
        var actual = upgrade(DataTypes.BLOCK_NAME, Value.wrap("minecraft:grass"), SHORT_GRASS_VERSION, 0);
        assertEquals("minecraft:grass", actual.value());
    }
}
