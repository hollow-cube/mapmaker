package net.hollowcube.datafix;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class BlockEntityUpgradeTest extends AbstractDataFixTest {
    @Test
    void testNoopFullUpgrade() {
        var actual = upgradeFull(DataTypes.BLOCK_ENTITY, wrap(Map.of(
                "id", "minecraft:nonexistent"
        )));
        assertEquals("minecraft:nonexistent", actual.getValue("id"));
    }

    @Test
    void testAnyEntityUpgrade() {
        // 1458 name upgrade only actually applies to nameable block entities but close enough.
        var actual = upgrade(DataTypes.BLOCK_ENTITY, wrap(Map.of(
                "id", "minecraft:dropper",
                "CustomName", "hi"
        )), 0, 1458);
        assertEquals("{\"text\":\"hi\"}", actual.getValue("CustomName"));
    }

    @Test
    void testJigsawBlockUpgrade() {
        var actual = upgradeFull(DataTypes.BLOCK_ENTITY, wrap(Map.of(
                "id", "minecraft:jigsaw_block",
                "target_pool", "my:pool"
        )));
        assertEquals("minecraft:jigsaw_block", actual.getValue("id"));
        assertNull(actual.getValue("target_pool"));
        assertEquals("my:pool", actual.getValue("pool"));
    }

    @Test
    void testIdSpecificUpgradeNotAppliedToOther() {
        // target_pool -> pool is a jigsaw specific upgrade, should not be applied to droppers
        var actual = upgradeFull(DataTypes.BLOCK_ENTITY, wrap(Map.of(
                "id", "minecraft:dropper",
                "target_pool", "my:pool"
        )));
        assertEquals("minecraft:dropper", actual.getValue("id"));
        assertEquals("my:pool", actual.getValue("target_pool"));
        assertNull(actual.getValue("pool"));
    }

    @Test
    void testIdSpecificUpgradeAppliedAfterIdChange() {
        // the loot table rename is applied to minecraft:chest, not to Chest.
        var actual = upgradeFull(DataTypes.BLOCK_ENTITY, wrap(Map.of(
                "id", "Chest",
                "LootTable", "minecraft:chests/village_blacksmith"
        )));
        assertEquals("minecraft:chest", actual.getValue("id"));
        assertEquals("minecraft:chests/village/village_weaponsmith", actual.getValue("LootTable"));
    }
}
