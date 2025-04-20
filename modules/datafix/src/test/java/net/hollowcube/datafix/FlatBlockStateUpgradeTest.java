package net.hollowcube.datafix;

import net.hollowcube.datafix.util.Value;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FlatBlockStateUpgradeTest extends AbstractDataFixTest {

    @Test
    void testDoublePropertiesRegression() { // Should NOT be upgraded since we already claim to be past the fix version
        var actual = upgrade(DataTypes.FLAT_BLOCK_STATE, Value.wrap("minecraft:oak_slab[type=top,waterlogged=false]"),
                0, 4318);
        assertEquals("minecraft:oak_slab[type=top,waterlogged=false]", actual.value());
    }
}
