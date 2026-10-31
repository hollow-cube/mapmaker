package net.hollowcube.terraform.apply;

import net.hollowcube.terraform.TestInstances;
import net.hollowcube.terraform.buffer.EditBuffer;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@EnvTest
class UnloadedChunkPolicyTest {

    // (1,70,1) is in the loaded chunk (0,0). (161,70,161) is in chunk (10,10), left unloaded.
    private static final int LOADED_X = 1, UNLOADED_X = 161, Y = 70;

    @Test
    void loadGeneratesAndAppliesUnloadedChunk(Env env) {
        Instance instance = TestInstances.flatChunk(env);

        ApplyEngine.ApplyResult result = ApplyEngine.DEFAULT.apply(
            instance,
            twoChunks(),
            UnloadedChunkPolicy.LOAD
        );

        assertEquals(2, result.changed());
        assertNotNull(instance.getChunk(10, 10), "target chunk loaded under LOAD");
        assertEquals(Block.STONE, instance.getBlock(UNLOADED_X, Y, UNLOADED_X));
    }

    @Test
    void failThrowsAndAppliesNothing(Env env) {
        Instance instance = TestInstances.flatChunk(env);

        assertThrows(
            IllegalStateException.class,
            () -> ApplyEngine.DEFAULT.apply(instance, twoChunks(), UnloadedChunkPolicy.FAIL)
        );
        assertEquals(
            Block.AIR,
            instance.getBlock(LOADED_X, Y, LOADED_X),
            "pre-check throws before any write"
        );
        assertNull(instance.getChunk(10, 10));
    }

    @Test
    void failAppliesWhenAllChunksLoaded(Env env) {
        Instance instance = TestInstances.flatChunk(env);
        EditBuffer buffer = EditBuffer.builder()
            .setBlockState(LOADED_X, Y, LOADED_X, Block.STONE)
            .build();

        ApplyEngine.ApplyResult result = ApplyEngine.DEFAULT.apply(
            instance,
            buffer,
            UnloadedChunkPolicy.FAIL
        );

        assertEquals(1, result.changed());
        assertEquals(Block.STONE, instance.getBlock(LOADED_X, Y, LOADED_X));
    }

    private static EditBuffer twoChunks() {
        return EditBuffer.builder()
            .setBlockState(LOADED_X, Y, LOADED_X, Block.STONE)
            .setBlockState(UNLOADED_X, Y, UNLOADED_X, Block.STONE)
            .build();
    }
}
