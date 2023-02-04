package net.hollowcube.terraform.mask;

import net.hollowcube.terraform.action.edit.MockWorldView;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestOffsetMask {

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 1, 2})
    public void testOffsetMask(int i) {
        var world = MockWorldView.of(Block.AIR); // Doesn't matter for this test, but it will be accessed.

        // Always match if inner mask always matches
        var mask = new OffsetMask(Mask.always(), 0, i, 0);
        assertTrue(mask.test(world, Vec.ZERO, Block.AIR));

        // Never match if inner mask never matches
        mask = new OffsetMask(Mask.never(), 0, i, 0);
        assertFalse(mask.test(world, Vec.ZERO, Block.AIR));
    }

    @Test
    public void testOffsetMaskOverlay() {
        // "real world" test of overlay mask

        var world = MockWorldView.builder()
                .set(0, 0, 0, Block.STONE)
                .build();
        var mask = OffsetMask.overlay(new BlockMask(Block.STONE.id()));

        assertTrue(mask.test(world, new Vec(0, 1, 0), Block.AIR));
    }

}
