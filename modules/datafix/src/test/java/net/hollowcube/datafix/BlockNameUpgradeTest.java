package net.hollowcube.datafix;

import net.hollowcube.datafix.util.Value;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BlockNameUpgradeTest extends AbstractDataFixTest {
    private static final int SHORT_GRASS_VERSION = 3692;

    @ValueSource(strings = {
            // Grass becomes the block during flattening, new grass is short_grass. but we never see that here.
            "minecraft:grass->minecraft:grass_block"
    })
    @ParameterizedTest
    void testV99ToV4314(String input) {
        var split = input.split("->");
        assertEquals(2, split.length);
        var result = upgrade(DataTypes.BLOCK_NAME, Value.wrap(split[0]), 0, 4314);
        assertEquals(split[1], result.value());
    }

    @Test
    void testGrassToShortGrassUpgrade() {
        var actual = upgrade(DataTypes.BLOCK_NAME, Value.wrap("minecraft:grass"), 2000, SHORT_GRASS_VERSION);
        // Starting post flattening results in short_grass correctly
        assertEquals("minecraft:short_grass", actual.value());
    }

    @Test
    void testShortGrassUpgradeToOneBefore() { // Should NOT be upgraded
        var actual = upgrade(DataTypes.BLOCK_NAME, Value.wrap("minecraft:grass"), 2000, SHORT_GRASS_VERSION - 1);
        assertEquals("minecraft:grass", actual.value());
    }

    @Test
    void testShortGrassUpgradeAfterToAfter() { // Should NOT be upgraded since we already claim to be past the fix version
        var actual = upgrade(DataTypes.BLOCK_NAME, Value.wrap("minecraft:grass"), SHORT_GRASS_VERSION, Integer.MAX_VALUE);
        assertEquals("minecraft:grass", actual.value());
    }
}
