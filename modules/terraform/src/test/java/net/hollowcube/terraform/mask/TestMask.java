package net.hollowcube.terraform.mask;

import net.hollowcube.terraform.action.edit.MockWorldView;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestMask {

    @Test
    public void testNot() {
        assertFalse(Mask.not(Mask.always()).test(MockWorldView.none(), Vec.ZERO, Block.AIR));
        assertTrue(Mask.not(Mask.never()).test(MockWorldView.none(), Vec.ZERO, Block.AIR));
    }

    @Test
    public void testAnd() {
        assertTrue(Mask.and(Mask.always(), Mask.always()).test(MockWorldView.none(), Vec.ZERO, Block.AIR));
        assertFalse(Mask.and(Mask.always(), Mask.never()).test(MockWorldView.none(), Vec.ZERO, Block.AIR));
        assertFalse(Mask.and(Mask.never(), Mask.always()).test(MockWorldView.none(), Vec.ZERO, Block.AIR));
        assertFalse(Mask.and(Mask.never(), Mask.never()).test(MockWorldView.none(), Vec.ZERO, Block.AIR));

        assertFalse(Mask.and(Mask.always(), Mask.never(), MockMask.none()).test(MockWorldView.none(), Vec.ZERO, Block.AIR));
    }

    @Test
    public void testOr() {
        assertTrue(Mask.or(Mask.always(), Mask.always()).test(MockWorldView.none(), Vec.ZERO, Block.AIR));
        assertTrue(Mask.or(Mask.always(), Mask.never()).test(MockWorldView.none(), Vec.ZERO, Block.AIR));
        assertTrue(Mask.or(Mask.never(), Mask.always()).test(MockWorldView.none(), Vec.ZERO, Block.AIR));
        assertFalse(Mask.or(Mask.never(), Mask.never()).test(MockWorldView.none(), Vec.ZERO, Block.AIR));

        assertTrue(Mask.or(Mask.never(), Mask.never(), Mask.always()).test(MockWorldView.none(), Vec.ZERO, Block.AIR));
    }

}
