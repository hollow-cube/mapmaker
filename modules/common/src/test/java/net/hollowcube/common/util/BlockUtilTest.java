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

    @Test
    void suffocatingBlocks() {
        assertTrue(BlockUtil.isSuffocating(Block.STONE));
        assertTrue(BlockUtil.isSuffocating(Block.BARRIER));
        assertTrue(BlockUtil.isSuffocating(Block.SOUL_SAND));
        assertFalse(BlockUtil.isSuffocating(Block.GLASS));
        assertFalse(BlockUtil.isSuffocating(Block.OAK_LEAVES));
        assertFalse(BlockUtil.isSuffocating(Block.OAK_SLAB));
        assertFalse(BlockUtil.isSuffocating(Block.AIR));
    }
}
