package net.hollowcube.datafix;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EntityUpgradeTest extends AbstractDataFixTest {
    @Test
    void testNoopFullUpgrade() {
        var actual = upgradeFull(DataTypes.ENTITY, wrap(Map.of(
                "id", "minecraft:nonexistent"
        )));
        assertEquals("minecraft:nonexistent", actual.getValue("id"));
    }
}
