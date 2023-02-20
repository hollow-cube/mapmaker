package net.hollowcube.terraform.mask.script;

import net.hollowcube.terraform.mask.BlockMask;
import net.minestom.server.instance.block.Block;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestTreeBlockState {

    @Test
    public void testValidNoProps() {
        var tree = new Tree.BlockState(0, 5, -1, -1, "stone", null);
        var result = (BlockMask) assertDoesNotThrow(tree::toMask);
        assertEquals(Block.STONE.id(), result.blockId());
        assertEquals(0, result.properties().size());
    }

    @Test
    public void testValidNoPropsWithBrackets() {
        var tree = new Tree.BlockState(0, 7, 5, 6, "stone", List.of());
        var result = (BlockMask) assertDoesNotThrow(tree::toMask);
        assertEquals(Block.STONE.id(), result.blockId());
        assertEquals(0, result.properties().size());
    }

    @Test
    public void testInvalidUnfinishedBracket() {
        var tree = new Tree.BlockState(0, 6, 5, -1, "stone", null);
        var result = assertThrows(MaskParseException.class, tree::toMask);
        assertEquals(6, result.start());
        assertEquals(6, result.end());
        assertEquals("Expected property or ']'", result.getMessage());
    }

    @Test
    public void testValidWithProps() {
        var tree = new Tree.BlockState(0, 26, 5, 25, "stone_stairs",
                List.of(new Tree.BlockState.Property(6, 24, "facing", "north")));
        var result = (BlockMask) assertDoesNotThrow(tree::toMask);
        assertEquals(Block.STONE_STAIRS.id(), result.blockId());
        assertEquals(1, result.properties().size());
        assertEquals("north", result.properties().get("facing"));
    }

    @Test
    public void testInvalidUnfinishedProp() {
        var tree = new Tree.BlockState(0, 21, 5, 20, "stone_stairs",
                List.of(new Tree.BlockState.Property(6, 19, "facing", null)));
        var result = assertThrows(MaskParseException.class, tree::toMask);
        assertEquals(19, result.start());
        assertEquals(19, result.end());
        assertEquals("Expected value", result.getMessage());
    }

    @Test
    public void testInvalidPropValue() {
        var tree = new Tree.BlockState(0, 28, 5, 27, "stone_stairs",
                List.of(new Tree.BlockState.Property(6, 26, "facing", "nowhere")));
        var result = assertThrows(MaskParseException.class, tree::toMask);
        assertEquals(6, result.start());
        assertEquals(26, result.end());
        assertEquals("minecraft:stone_stairs has no property facing=nowhere", result.getMessage());
    }

}
