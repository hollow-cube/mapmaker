package net.hollowcube.terraform.mask.script;

import net.hollowcube.terraform.mask.BlockMask;
import net.minestom.server.instance.block.Block;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
    public void testValidWithProps() {
//        var tree = new Tree.BlockState(0, 7, 5, 6, "stone_stairs",
//                List.of(new Tree.BlockState.Property("facing", "north")));
//        var result = (BlockMask) assertDoesNotThrow(tree::toMask);
//        assertEquals(Block.STONE.id(), result.blockId());
//        assertEquals(0, result.properties().size());
    }


    // Valid with props
    // Invalid (not a real block)
    // Invalid (open bracket then nothing)
}
