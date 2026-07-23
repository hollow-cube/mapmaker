package net.hollowcube.terraform.util.transformations;

import net.hollowcube.schem.builder.SchematicBuilder;
import net.hollowcube.terraform.session.Clipboard;
import net.hollowcube.test.ServerTest;
import net.hollowcube.test.TestEnv;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ServerTest
class TestFlipSchematicTransformation {

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
