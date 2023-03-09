package net.hollowcube.terraform.instance;

import net.hollowcube.util.schem.Rotation;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.test.Env;
import net.minestom.server.test.EnvTest;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;


@SuppressWarnings("JUnitMalformedDeclaration")
@EnvTest
public class TestSchemChunkBatchIntegration {

    @Test
    public void testApplyUnloadedChunk(Env env) {
        var batch = new SchemChunkBatch();
        batch.setBlock(0, 0, 0, Block.TNT);

        var instance = env.createFlatInstance();
        assertTrue(batch.apply(instance, 0, 0).isCompletedExceptionally());
    }

    @Test
    public void testBasicApply(Env env) {
        var batch = new SchemChunkBatch();
        batch.setBlock(0, 0, 0, Block.TNT);

        var instance = env.createFlatInstance();
        instance.loadChunk(0, 0).join();

        var schematic = batch.apply(instance, 0, 0).join();

        // Should have added the block to the instance
        assertEquals(Block.TNT, instance.getBlock(0, 0, 0));

        var blockCount = new AtomicInteger();
        schematic.apply(Rotation.NONE, (point, block) -> {
            blockCount.incrementAndGet();

            // Should have removed the block from the schematic
            assertEquals(Vec.ZERO, point);
            assertEquals(Block.STONE, block);
        });
        assertEquals(1, blockCount.get());
    }

    @Test
    public void testBasicApply2(Env env) {
        var batch = new SchemChunkBatch();
        batch.setBlock(5, 0, 0, Block.TNT);

        var instance = env.createFlatInstance();
        instance.loadChunk(0, 0).join();

        var schematic = batch.apply(instance, 0, 0).join();

        // Should have added the block to the instance
        assertEquals(Block.TNT, instance.getBlock(5, 0, 0));

        var blockCount = new AtomicInteger();
        schematic.apply(Rotation.NONE, (point, block) -> {
            blockCount.incrementAndGet();

            // Should have removed the block from the schematic
            assertEquals(new Vec(5, 0, 0), point);
            assertEquals(Block.STONE, block);
        });
        assertEquals(1, blockCount.get());
    }
}
