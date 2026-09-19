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
        assertEquals("minecraft:nonexistent", actual.getValue("id"));
    }

    @Test
    void testShortGrassFullUpgrade() {
        var actual = upgradeFull(DataTypes.BLOCK_STATE, wrap(Map.of(
                "Name", "minecraft:grass"
        )));
        assertEquals("minecraft:short_grass", actual.getValue("id"));
    }

    @Test
    void testBlockStatePropertyChange() {
        var actual = upgradeFull(DataTypes.BLOCK_STATE, wrap(Map.of(
                "Name", "minecraft:jigsaw",
                "Properties", wrap(Map.of(
                        "facing", "south"
                ))
        )));
        assertEquals("minecraft:jigsaw", actual.getValue("id"));
        assertNull(actual.get("properties").getValue("facing"));
        assertEquals("south_up", actual.get("properties").getValue("orientation"));
    }

    @Test
    void testBlockStateFieldNamesRename() {
        var actual = upgrade(DataTypes.BLOCK_STATE, wrap(Map.of(
                "Name", "minecraft:oak_log",
                "Properties", wrap(Map.of(
                        "axis", "x"
                ))
        )), 4903, 5006);
        assertNull(actual.getValue("Name"));
        assertNull(actual.getValue("Properties"));
        assertEquals("minecraft:oak_log", actual.getValue("id"));
        assertEquals("x", actual.get("properties").getValue("axis"));
    }

    @Test
    void testBlockStateFieldNamesBeforeRename() {
        var actual = upgrade(DataTypes.BLOCK_STATE, wrap(Map.of(
                "Name", "minecraft:oak_log"
        )), 4903, 5005);
        assertEquals("minecraft:oak_log", actual.getValue("Name"));
        assertNull(actual.getValue("id"));
    }
}
