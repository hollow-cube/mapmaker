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

    @Test
    void testSchemaTransferUpgrade() {
        var actual = upgradeFull(DataTypes.ENTITY, wrap(Map.of(
                "id", "minecraft:enderman",
                "carried", "minecraft:grass"
        )));
        assertEquals("minecraft:enderman", actual.getValue("id"));
        assertEquals("minecraft:grass_block", actual.get("carriedBlockState").getValue("Name"));
    }
}
