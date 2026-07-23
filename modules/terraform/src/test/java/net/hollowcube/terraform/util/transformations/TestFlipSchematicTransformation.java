package net.hollowcube.terraform.util.transformations;

import net.hollowcube.schem.builder.SchematicBuilder;
import net.hollowcube.terraform.session.Clipboard;
import net.hollowcube.test.ServerTest;
import net.hollowcube.test.TestEnv;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ServerTest
class TestFlipSchematicTransformation {

    private static final SchematicTransformation FLIP_X = SchematicTransformation.flip(true, false, false);
    private static final SchematicTransformation FLIP_Y = SchematicTransformation.flip(false, true, false);
    private static final SchematicTransformation FLIP_Z = SchematicTransformation.flip(false, false, true);
    private static final SchematicTransformation FLIP_XZ = SchematicTransformation.flip(true, false, true);

    private static Stream<Arguments> blockFlipSupplier() {
        return Stream.of(
                Arguments.of("Facing mirrored", FLIP_X,
                        Block.FURNACE.withProperty("facing", "east"),
                        Block.FURNACE.withProperty("facing", "west")),
                Arguments.of("Sign rotation mirrored", FLIP_X,
                        Block.OAK_SIGN.withProperty("rotation", "4"),
                        Block.OAK_SIGN.withProperty("rotation", "12")),
                Arguments.of("Fence connections swapped", FLIP_X,
                        Block.ACACIA_FENCE.withProperties(Map.of("east", "true", "west", "false", "north", "true")),
                        Block.ACACIA_FENCE.withProperties(Map.of("east", "false", "west", "true", "north", "true"))),
                Arguments.of("Stair facing and shape mirrored", FLIP_X,
                        Block.OAK_STAIRS.withProperties(Map.of("facing", "east", "shape", "inner_left")),
                        Block.OAK_STAIRS.withProperties(Map.of("facing", "west", "shape", "inner_right"))),
                Arguments.of("Double horizontal flip preserves chirality", FLIP_XZ,
                        Block.OAK_STAIRS.withProperties(Map.of("facing", "east", "shape", "inner_left")),
                        Block.OAK_STAIRS.withProperties(Map.of("facing", "west", "shape", "inner_left"))),
                Arguments.of("Door hinge mirrored", FLIP_Z,
                        Block.OAK_DOOR.withProperties(Map.of("facing", "north", "hinge", "left")),
                        Block.OAK_DOOR.withProperties(Map.of("facing", "south", "hinge", "right"))),
                Arguments.of("Stair half swapped vertically", FLIP_Y,
                        Block.OAK_STAIRS.withProperties(Map.of("facing", "east", "half", "bottom")),
                        Block.OAK_STAIRS.withProperties(Map.of("facing", "east", "half", "top"))),
                Arguments.of("Rail corner mirrored", FLIP_X,
                        Block.RAIL.withProperty("shape", "south_east"),
                        Block.RAIL.withProperty("shape", "south_west")),
                Arguments.of("Invalid mirrored value kept as-is", FLIP_Y,
                        Block.HOPPER.withProperty("facing", "down"),
                        Block.HOPPER.withProperty("facing", "down"))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("blockFlipSupplier")
    void testBlockFlip(String name, SchematicTransformation transformation, Block input, Block expected) {
        assertEquals(expected, transformation.apply(input));
    }

    // Regression test for #799: selection x=[10..14] copied while standing at
    // x=21 gives copy-relative positions x=[-11..-7]. Flipping must mirror the
    // strip across the copy origin to x=[7..11], not translate it further out.
    @Test
    void testFlipMirrorsAcrossCopyOrigin(TestEnv env) {
        var builder = SchematicBuilder.builder();
        for (int x = -11; x <= -7; x++) {
            builder.block(new Vec(x, 0, 0), x == -7 ? Block.STONE : Block.DIRT);
        }

        var clipboard = new Clipboard("test");
        clipboard.setData(builder.build());
        clipboard.transform(SchematicTransformation.flip(true, false, false));

        var flipped = clipboard.getTransformedSchematic();
        assertEquals(new Vec(7, 0, 0), flipped.offset());
        flipped.forEachBlock((pos, block) ->
                assertEquals(pos.sameBlock(7, 0, 0) ? Block.STONE : Block.DIRT, block, pos.toString()));
    }
}
