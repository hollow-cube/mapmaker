package net.hollowcube.terraform.pattern;

import net.hollowcube.terraform.action.edit.MockWorldView;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TypeStatePatternTest {

    @Test
    void replaceStairToStairKeepState() {
        assertPatternResultEquals(
                new TypeStatePattern(Block.ACACIA_STAIRS.id(), Map.of()),
                Block.ACACIA_STAIRS.withProperty("facing", "west").withProperty("shape", "inner_left"),
                Block.OAK_STAIRS.withProperty("facing", "west").withProperty("shape", "inner_left")
        );
    }

    @Test
    void removeWaterlogged() {
        assertPatternResultEquals(
                new TypeStatePattern(-1, Map.of("waterlogged", "false")),
                Block.IRON_BARS,
                Block.IRON_BARS.withProperty("waterlogged", "true")
        );
    }

    @Test
    void removeWaterloggedNoop() {
        assertPatternResultEquals(
                new TypeStatePattern(-1, Map.of()),
                Block.STONE, Block.STONE
        );
    }

    @Test
    void doubleSlab() {
        assertPatternResultEquals(
                new TypeStatePattern(-1, Map.of("type", "double")),
                Block.STONE_SLAB.withProperty("type", "double"),
                Block.STONE_SLAB
        );
    }

    @Test
    void doubleSlabNoop() {
        assertPatternResultEquals(
                new TypeStatePattern(-1, Map.of("type", "double")),
                Block.STONE_SLAB.withProperty("type", "double"),
                Block.STONE_SLAB.withProperty("type", "double")
        );
    }

    @Test
    void changeTypeAndProperty() {
        assertPatternResultEquals(
                new TypeStatePattern(Block.ACACIA_STAIRS.id(), Map.of("shape", "inner_left")),
                Block.ACACIA_STAIRS.withProperty("facing", "east").withProperty("shape", "inner_left"),
                Block.OAK_STAIRS.withProperty("facing", "east")
        );
    }

    @Test
    void changeTypeAndPropertyInvalidProperty() {
        assertPatternResultEquals(
                new TypeStatePattern(Block.ACACIA_STAIRS.id(), Map.of("shape", "inner_left")),
                Block.ACACIA_STAIRS.withProperty("facing", "east").withProperty("shape", "inner_left"),
                Block.DROPPER.withProperty("facing", "east")
        );
    }

    static void assertPatternResultEquals(@NotNull Pattern pattern, @NotNull Block expected, @NotNull Block input) {
        var result = pattern.blockAt(MockWorldView.of(input), Vec.ZERO);
        assertEquals(expected, result);
    }


}
