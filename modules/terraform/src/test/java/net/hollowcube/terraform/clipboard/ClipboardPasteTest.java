package net.hollowcube.terraform.clipboard;

import net.hollowcube.terraform.TestInstances;
import net.hollowcube.terraform.apply.ApplyEngine;
import net.hollowcube.terraform.buffer.EditBuffer;
import net.hollowcube.terraform.util.Transform;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@EnvTest
class ClipboardPasteTest {

    @Test
    void copyRotatePasteOrientsAndUndoes(Env env) {
        Instance instance = TestInstances.flatChunk(env);
        Block east = Block.OAK_STAIRS.withProperty("facing", "east");
        instance.setBlock(5, 70, 5, east);
        Block before = instance.getBlock(8, 70, 5);

        Point origin = new Vec(5, 70, 5);
        Clipboard clip = copy(instance, origin, origin).withTransform(Transform.rotateY(90));

        var result = ApplyEngine.DEFAULT.apply(
            instance,
            clip.pasteBuffer(new Vec(8, 70, 5), false)
        );
        assertEquals(Block.OAK_STAIRS.withProperty("facing", "south"), instance.getBlock(8, 70, 5));
        assertEquals(east, instance.getBlock(5, 70, 5));

        ApplyEngine.DEFAULT.apply(instance, result.undo());
        assertEquals(before, instance.getBlock(8, 70, 5));
    }

    @Test
    void flipPasteMirrorsState(Env env) {
        Instance instance = TestInstances.flatChunk(env);
        instance.setBlock(5, 70, 5, Block.OAK_STAIRS.withProperty("facing", "east"));

        Point origin = new Vec(5, 70, 5);
        Clipboard clip = copy(
            instance,
            origin,
            origin
        ).withTransform(Transform.mirror(Transform.Axis.X));

        ApplyEngine.DEFAULT.apply(instance, clip.pasteBuffer(new Vec(8, 70, 5), false));
        assertEquals(Block.OAK_STAIRS.withProperty("facing", "west"), instance.getBlock(8, 70, 5));
    }

    @Test
    void fullTurnPasteRoundTrips(Env env) {
        Instance instance = TestInstances.flatChunk(env);
        instance.setBlock(2, 70, 2, Block.STONE);
        instance.setBlock(3, 70, 3, Block.DIRT);

        Point origin = new Vec(2, 70, 2);
        Clipboard clip = copy(instance, origin, new Vec(3, 70, 3));
        instance.setBlock(2, 70, 2, Block.AIR);
        instance.setBlock(3, 70, 3, Block.AIR);

        ApplyEngine.DEFAULT.apply(
            instance,
            clip.withTransform(Transform.rotateY(360)).pasteBuffer(origin, false)
        );
        assertEquals(Block.STONE, instance.getBlock(2, 70, 2));
        assertEquals(Block.DIRT, instance.getBlock(3, 70, 3));
    }

    @Test
    void rotatedPasteAcrossAChunkBorderLandsEveryCell(Env env) {
        Instance instance = TestInstances.flat(env);
        instance.setBlock(14, 70, 2, Block.STONE);
        instance.setBlock(15, 70, 2, Block.DIRT);
        instance.setBlock(15, 71, 3, Block.GLASS);

        Point origin = new Vec(14, 70, 2);
        Clipboard clip = copy(
            instance,
            origin,
            new Vec(15, 71, 3)
        ).withTransform(Transform.rotateY(180));

        ApplyEngine.DEFAULT.apply(instance, clip.pasteBuffer(new Vec(16, 70, 20), false));
        assertEquals(Block.STONE, instance.getBlock(16, 70, 20));
        assertEquals(Block.DIRT, instance.getBlock(15, 70, 20));
        assertEquals(Block.GLASS, instance.getBlock(15, 71, 19));
    }

    /// The inclusive box `a`-`b` as a clipboard, air included.
    private static Clipboard copy(Instance instance, Point a, Point b) {
        EditBuffer.Builder builder = EditBuffer.builder();
        for (int x = a.blockX(); x <= b.blockX(); x++)
            for (int y = a.blockY(); y <= b.blockY(); y++)
                for (int z = a.blockZ(); z <= b.blockZ(); z++)
                    builder.setBlockState(x, y, z, instance.getBlock(x, y, z));
        return new Clipboard(builder.build(), a);
    }
}
