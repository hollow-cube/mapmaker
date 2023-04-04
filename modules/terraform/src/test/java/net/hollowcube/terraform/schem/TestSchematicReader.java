package net.hollowcube.terraform.schem;

import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.*;

class TestSchematicReader {

    @Test
    void happyCase() {
        var is = getClass().getResourceAsStream("/small.schem");
        assertNotNull(is);
        var schem = SchematicReader.read(is);

        assertEquals(new Vec(2, 2, 2), schem.size());
        assertEquals(new Vec(2, 0, 0), schem.offset());

        var expectedBlocks = new byte[]{0, 0, 0, 0, 1, 1, 1, 1};
        assertArrayEquals(expectedBlocks, schem.blocks());

        assertEquals(Block.OAK_LOG, schem.palette()[0]);
        assertEquals(Block.BLACK_WOOL, schem.palette()[1]);
    }

    @Test
    void testInvalidFile() {
        var bytes = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        var is = new ByteArrayInputStream(bytes);
        assertThrows(SchematicReadException.class, () -> SchematicReader.read(is));
    }

}
