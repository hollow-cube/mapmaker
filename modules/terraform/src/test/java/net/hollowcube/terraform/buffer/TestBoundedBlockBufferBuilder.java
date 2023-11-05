package net.hollowcube.terraform.buffer;

import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class TestBoundedBlockBufferBuilder extends BaseBufferBuilderTest {

    @Override
    @NotNull BlockBuffer.Builder createBuilder() {
        return new BoundedBlockBufferBuilder(
                null,
                new Vec(-100, -100, -100),
                new Vec(100, 100, 100)
        );
    }

    @Test
    void testPosOutsideBoundary() {
        var buffer = new BoundedBlockBufferBuilder(null, Vec.ZERO, Vec.ZERO);
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;

                    final int fx = x, fy = y, fz = z;
                    assertThrows(IllegalArgumentException.class, () -> buffer.set(fx, fy, fz, 1));
                }
            }
        }
    }

}
