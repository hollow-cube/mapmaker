package net.hollowcube.terraform.mask;

import net.hollowcube.terraform.action.edit.MockWorldView;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.Direction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestSurfaceMask {

    private static Stream<Arguments> singleBlockAloneSupplier() {
        return Stream.of(
                Arguments.of("solid", Block.STONE, true),
                Arguments.of("alt solid", Block.BIRCH_TRAPDOOR, true),
                Arguments.of("liquid", Block.WATER, false),
                Arguments.of("air", Block.AIR, false),
                Arguments.of("non-solid", Block.SHORT_GRASS, false)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("singleBlockAloneSupplier")
    public void testSingleBlockAlone(String name, Block block, boolean matches) {
        var world = MockWorldView.builder()
                .defaultBlock(Block.AIR)
                .set(0, 0, 0, block)
                .build();
        var result = new SurfaceMask().test(world, new Vec(0, 0, 0), block);
        if (matches) {
            assertTrue(result, "expected match, but was not");
        } else {
            assertFalse(result, "expected no match, but was");
        }
    }

    @ParameterizedTest
    @EnumSource(Direction.class)
    public void testSingleExposedFace(Direction direction) {
        var world = MockWorldView.builder()
                .fill(0, 0, 0, 3, 3, 3, Block.STONE)
                .set(1 + direction.normalX(), 1 + direction.normalY(), 1 + direction.normalZ(), Block.AIR)
                .build();
        var result = new SurfaceMask().test(world, new Vec(1, 1, 1), Block.STONE);
        assertTrue(result, "expected match, but was not");
    }

    @Test
    public void testNoExposedFaces() {
        var world = MockWorldView.builder()
                .fill(0, 0, 0, 3, 3, 3, Block.STONE)
                .build();
        var result = new SurfaceMask().test(world, new Vec(1, 1, 1), Block.STONE);
        assertFalse(result, "expected no match, but was");
    }
}
