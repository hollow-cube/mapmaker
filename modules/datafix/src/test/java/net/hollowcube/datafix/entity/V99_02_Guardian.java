package net.hollowcube.datafix.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class V99_02_Guardian extends AbstractEntityUpgradeTest {
    private static final int CURRENT = 4314;

    @Test
    void idSplitToElderGuardian() {
        var result = upgradeC(0, CURRENT);
        assertEquals("minecraft:elder_guardian", result.getString("id"));
    }

    @Test
    void customNameToTextString() {
        var result = upgradeC(0, 3000);
        var customName = result.getString("CustomName");
        assertEquals("{\"text\":\"DA PIG\"}", customName);
    }

    @Test
    void customNameToTextObject() {
        var result = upgradeC(0, CURRENT);
        var customName = result.getCompound("CustomName");
        assertEquals("DA PIG", customName.getString("text"));
    }


}
