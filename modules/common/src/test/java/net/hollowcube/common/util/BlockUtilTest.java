package net.hollowcube.common.util;

import net.minestom.server.instance.block.Block;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockUtilTest {

    @Test
    void blockRestitutionMatchesVanilla() {
        assertEquals(1.0, BlockUtil.blockRestitution(Block.SLIME_BLOCK));
        assertEquals(0.75, BlockUtil.blockRestitution(Block.RED_BED));
        assertEquals(0.75, BlockUtil.blockRestitution(Block.SHELF_MUSHROOM));
        assertEquals(0.0, BlockUtil.blockRestitution(Block.STRAW_BED));
        assertEquals(0.0, BlockUtil.blockRestitution(Block.STONE));
    }

    @Test
    void honeySuppressesBounce() {
        assertTrue(BlockUtil.suppressesBounce(Block.HONEY_BLOCK));
        assertFalse(BlockUtil.suppressesBounce(Block.SLIME_BLOCK));
    }
}
