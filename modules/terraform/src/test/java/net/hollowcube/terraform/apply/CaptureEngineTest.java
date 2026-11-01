package net.hollowcube.terraform.apply;

import net.hollowcube.terraform.TestInstances;
import net.hollowcube.terraform.buffer.EditBuffer;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.junit.jupiter.api.Test;

import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.*;

@EnvTest
class CaptureEngineTest {
    private static final Block[] BLOCKS = {
        Block.AIR,
        Block.STONE,
        Block.DIRT,
        Block.OAK_STAIRS.withProperty("facing", "east"),
    };

    @Test
    void capturesEveryCellOfTheBoxLikeAPerCellRead(Env env) {
        Instance instance = TestInstances.flat(env, -2, 1, -2, 1);
        var random = new SplittableRandom(3);
        for (int x = -32; x < 32; x++) {
            for (int z = -32; z < 32; z++) {
                for (int y = 30; y < 70; y++)
                    instance.setBlock(x, y, z, BLOCKS[random.nextInt(BLOCKS.length)]);
            }
        }
        Block chest = Block.CHEST.withNbt(
            CompoundBinaryTag.builder().putString("Lock", "k").build()
        );
        instance.setBlock(-17, 48, 5, chest);
        instance.setBlock(0, 63, 0, chest);

        // Partial sections on every side of a run of whole ones, across negative coordinates.
        var box = Area.cuboid(new BlockVec(-21, 33, -18), new BlockVec(19, 66, 17));
        EditBuffer captured = CaptureEngine.capture(instance, box);

        for (int x = -32; x < 32; x++) {
            for (int z = -32; z < 32; z++) {
                for (int y = 30; y < 70; y++) {
                    if (box.contains(x, y, z)) {
                        Block world = instance.getBlock(x, y, z);
                        assertEquals(world, captured.getBlock(x, y, z), x + ", " + y + ", " + z);
                    } else {
                        assertEquals(
                            EditBuffer.NONE,
                            captured.getBlockState(x, y, z),
                            x + ", " + y + ", " + z
                        );
                    }
                }
            }
        }
        assertEquals(box, captured.blockBounds());
    }

    @Test
    void wholeSectionsKeepTheirBlockEntities(Env env) {
        Instance instance = TestInstances.flatChunk(env);
        Block chest = Block.CHEST.withNbt(CompoundBinaryTag.builder().putInt("x", 1).build());
        instance.setBlock(3, 70, 4, chest);

        EditBuffer captured = CaptureEngine.capture(
            instance,
            Area.cuboid(new BlockVec(0, 64, 0), new BlockVec(15, 79, 15))
        );

        assertEquals(chest, captured.getBlock(3, 70, 4));
        assertEquals(Block.AIR, captured.getBlock(3, 71, 4));
    }

    @Test
    void clipsToTheWorldsHeightAndLoadsChunks(Env env) {
        Instance instance = TestInstances.flatChunk(env);
        int minY = instance.getCachedDimensionType().minY();

        EditBuffer captured = CaptureEngine.capture(
            instance,
            Area.cuboid(new BlockVec(40, minY - 20, 40), new BlockVec(41, minY + 1, 41))
        );

        assertNotNull(instance.getChunk(2, 2));
        assertEquals(
            Area.cuboid(new BlockVec(40, minY, 40), new BlockVec(41, minY + 1, 41)),
            captured.blockBounds()
        );
    }
}
