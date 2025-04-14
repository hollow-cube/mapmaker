package net.hollowcube.datafix;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class BlockStateUpgradeTest extends AbstractDataFixTest {
    @Test
    void testNoopFullUpgrade() {
        var actual = upgradeFull(DataTypes.BLOCK_STATE, wrap(Map.of(
                "Name", "minecraft:nonexistent"
        )));
        assertEquals("minecraft:nonexistent", actual.getValue("Name"));
    }

    @Test
    void testShortGrassFullUpgrade() {
        var actual = upgradeFull(DataTypes.BLOCK_STATE, wrap(Map.of(
                "Name", "minecraft:grass"
        )));
        assertEquals("minecraft:short_grass", actual.getValue("Name"));
    }

    @Test
    void testBlockStatePropertyChange() {
        var actual = upgradeFull(DataTypes.BLOCK_STATE, wrap(Map.of(
                "Name", "minecraft:jigsaw",
                "Properties", wrap(Map.of(
                        "facing", "south"
                ))
        )));
        assertEquals("minecraft:jigsaw", actual.getValue("Name"));
        assertNull(actual.get("Properties").getValue("facing"));
        assertEquals("south_up", actual.get("Properties").getValue("orientation"));
    }
}
