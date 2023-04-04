package net.hollowcube.terraform.schem;

import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TestSchematicBuilder {

    @Test
    void testBasicSchem() {
        var builder = new SchematicBuilder();
        builder.addBlock(0, 0, 0, Block.OAK_LOG);
        builder.addBlock(1, 0, 0, Block.OAK_LOG);
        builder.addBlock(0, 0, 1, Block.OAK_LOG);
        builder.addBlock(1, 0, 1, Block.OAK_LOG);
        builder.addBlock(0, 1, 0, Block.BLACK_WOOL);
        builder.addBlock(1, 1, 0, Block.BLACK_WOOL);
        builder.addBlock(0, 1, 1, Block.BLACK_WOOL);
        builder.addBlock(1, 1, 1, Block.BLACK_WOOL);

        var schem = builder.build();
        assertEquals(new Vec(2, 2, 2), schem.size());
        assertArrayEquals(new byte[]{0, 0, 0, 0, 1, 1, 1, 1}, schem.blocks());
        assertEquals(Block.OAK_LOG, schem.palette()[0]);
        assertEquals(Block.BLACK_WOOL, schem.palette()[1]);
    }

}
