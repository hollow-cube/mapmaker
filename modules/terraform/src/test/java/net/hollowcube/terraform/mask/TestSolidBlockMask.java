package net.hollowcube.terraform.mask;

import net.hollowcube.terraform.action.edit.MockWorldView;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestSolidBlockMask {

    @Test
    public void testSolidBlock() {
        // This test is kinda dumb since the implementation just uses Block#isSolid,
        // but is a sanity check in case it is changed in the future.

        var mask = new SolidBlockMask();
        var world = MockWorldView.none();

        for (var block : Block.values()) {
            if (block.isSolid()) {
                assertTrue(mask.test(world, Vec.ZERO, block));
            } else {
                assertFalse(mask.test(world, Vec.ZERO, block));
            }
        }

    }
}
