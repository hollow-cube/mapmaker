package net.hollowcube.datafix.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class V99_03_EntityHorse extends AbstractEntityUpgradeTest {
    private static final int CURRENT = 4314;

    @Test
    void idSplitToZombieHorse() {
        var result = upgradeC(0, CURRENT);
        assertEquals("minecraft:zombie_horse", result.getString("id"));
    }

}
