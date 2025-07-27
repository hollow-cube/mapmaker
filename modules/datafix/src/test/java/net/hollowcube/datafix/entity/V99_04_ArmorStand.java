package net.hollowcube.datafix.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class V99_04_ArmorStand extends AbstractEntityUpgradeTest {
    private static final int CURRENT = 4314;

    @Test
    void idUpgrade() {
        var result = upgradeC(0, CURRENT);
        assertEquals("minecraft:armor_stand", result.getString("id"));
    }

}
