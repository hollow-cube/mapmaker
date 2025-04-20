package net.hollowcube.datafix.blockEntity;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class V99_02_Cauldron extends AbstractBlockEntityUpgradeTest {
    private static final int CURRENT = 4314;

    @Test
    void upgradeId() {
        var result = upgradeC(0, CURRENT);
        assertEquals("minecraft:brewing_stand", result.getString("id"));
    }

    @Test
    void expectedKeys() {
        var result = upgradeC(0, CURRENT);
        assertEquals(Set.of("x", "y", "z", "id", "Items", "Lock", "BrewTime"), result.keySet());
    }

}
