package net.hollowcube.terraform.instance;

import net.hollowcube.terraform.give_me_new_home.instance.SchemBlockBatch;
import net.hollowcube.terraform.schem.Rotation;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;


@SuppressWarnings("JUnitMalformedDeclaration")
@EnvTest
class TestSchemBlockBatchIntegration {

    @Test
    void testBasicApply(Env env) {
        var batch = new SchemBlockBatch();
        batch.setBlock(0, 0, 0, Block.TNT);

        var instance = env.createFlatInstance();
        instance.loadChunk(0, 0).join();

        var schematic = batch.apply(instance).join();

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

//    @Test
//    public void testMultiChunkApply(Env env) {
//        var batch = new SchemBlockBatch();
//        batch.setBlock(0, 0, 0, Block.TNT);
//        batch.setBlock(25, 0, 0, Block.TNT);
//
//        var instance = env.createFlatInstance();
//        instance.loadChunk(0, 0).join();
//        instance.loadChunk(1, 0).join();
//
//        var schematic = batch.apply(instance).join();
//        assertEquals(new Vec(26, 1, 1), schematic.size());
//
//        // Should have added the block to the instance
//        assertEquals(Block.TNT, instance.getBlock(0, 0, 0));
//        assertEquals(Block.TNT, instance.getBlock(25, 0, 0));
//
//        var blockCount = new AtomicInteger();
//        schematic.apply(Rotation.NONE, (pos, block) -> {
//            blockCount.incrementAndGet();
//
//            if (pos.equals(Vec.ZERO)) {
//                assertEquals(Vec.ZERO, pos);
//                assertEquals(Block.STONE, block);
//            } else {
//                assertEquals(new Vec(25, 0, 0), pos);
//                assertEquals(Block.STONE, block);
//            }
//        });
//        assertEquals(2, blockCount.get());
//    }
}
