package net.hollowcube.datafix.blockEntity;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class V99_01_Banner extends AbstractBlockEntityUpgradeTest {
    private static final int CURRENT = 4314;

    @Test
    void upgradeId() {
        var result = upgradeC(0, CURRENT);
        assertEquals("minecraft:banner", result.getString("id"));
    }

    @Test
    void expectedKeys() {
        var result = upgradeC(0, CURRENT);
        assertEquals(Set.of("x", "y", "z", "id", "Base", "patterns"), result.keySet());
    }

    @Test
    void upgradeBase() {
        var result = upgradeC(0, CURRENT);
        assertEquals(6, result.getInt("Base"));
    }

    @Test
    void upgradePatterns() {
        var result = upgradeC(0, CURRENT);
        var patterns = result.getList("patterns");
        assertEquals(2, patterns.size());
        var first = patterns.getCompound(0);
        assertEquals("minecraft:stripe_top", first.getString("pattern"));
        assertEquals("red", first.getString("color"));
        var second = patterns.getCompound(1);
        assertEquals("minecraft:stripe_bottom", second.getString("pattern"));
        assertEquals("red", second.getString("color"));
    }

}
