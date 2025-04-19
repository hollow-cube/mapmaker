package net.hollowcube.datafix.blockEntity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class V99_04_FlowerPot extends AbstractBlockEntityUpgradeTest {
    private static final int CURRENT = 4314;

    @Test
    void upgradeId() {
        // doesnt really do much i suppose. this should get removed as a block entity from items which we can test elsewhere
        var result = upgradeC(0, CURRENT);
        assertEquals("minecraft:flower_pot", result.getString("id"));
    }

}
