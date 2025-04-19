package net.hollowcube.datafix.itemStack;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class V99_02_Record13 extends AbstractItemStackUpgradeTest {
    private static final int CURRENT = 4314;

    @Test
    void exactUpgrade() {
        var result = upgradeC(0, CURRENT);
        assertEquals("minecraft:music_disc_13", result.getString("id"));
        assertEquals(1, result.getInt("count"));

        assertEquals(Set.of("id", "count"), result.keySet());
    }
}
