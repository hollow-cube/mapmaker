package net.hollowcube.datafix.blockEntity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class V5002_01_HangingSign extends AbstractBlockEntityUpgradeTest {

    @Test
    void testAddAllowOpFeatures() {
        var result = upgradeC(4903, 5002);
        assertEquals(1, result.getByte("allow_op_features"));
    }

    @Test
    void testNotAddedBefore5002() {
        var result = upgradeC(4903, 5001);
        assertFalse(result.keySet().contains("allow_op_features"));
    }
}
