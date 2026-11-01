package net.hollowcube.terraform.apply;

import net.hollowcube.terraform.TestInstances;
import net.hollowcube.terraform.buffer.EditBuffer;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

@EnvTest
class ApplySubmitTest {
    private static final ApplyEngine ONE_COLUMN_PER_TICK = ApplyEngine.builder()
        .tickTarget(Duration.ZERO)
        .build();
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @Test
    void spansTicksWithinBudget(Env env) {
        Instance instance = TestInstances.flat(env);

        var result = ONE_COLUMN_PER_TICK.submit(instance, fill(0, 0, 31, 31, Block.STONE));
        assertFalse(result.isDone(), "nothing is written before the tick");
        env.tick();
        assertFalse(result.isDone(), "one column per tick at a zero target");
        assertEquals(Block.STONE, instance.getBlock(0, 70, 0));
        assertEquals(Block.AIR, instance.getBlock(31, 70, 31));

        env.tickWhile(() -> !result.isDone(), TIMEOUT);
        assertEquals(32 * 32, result.join().changed());
        assertEquals(Block.STONE, instance.getBlock(31, 70, 31));
    }

    @Test
    void usesTheRestOfTheTick(Env env) {
        Instance instance = TestInstances.flat(env);

        var result = ApplyEngine.DEFAULT.submit(instance, fill(0, 0, 31, 31, Block.STONE));
        env.tick();
        assertTrue(result.isDone(), "four small columns fit well inside one tick");
        var second = ApplyEngine.DEFAULT.submit(instance, fill(0, 0, 31, 31, Block.GLASS));
        env.tick();
        assertTrue(second.isDone(), "and still fit once the tick start is known");
    }

    @Test
    void appliesInSubmitOrder(Env env) {
        Instance instance = TestInstances.flat(env);

        var first = ONE_COLUMN_PER_TICK.submit(instance, fill(0, 0, 31, 31, Block.STONE));
        var second = ONE_COLUMN_PER_TICK.submit(instance, fill(0, 0, 0, 0, Block.GLASS));
        env.tickWhile(() -> !second.isDone(), TIMEOUT);

        assertTrue(first.isDone());
        assertEquals(Block.GLASS, instance.getBlock(0, 70, 0));
        assertEquals(Block.STONE, second.join().undo().getBlock(0, 70, 0));
    }

    @Test
    void loadsUnloadedTargetsOffTheTick(Env env) {
        Instance instance = TestInstances.flatChunk(env);

        var result = ApplyEngine.DEFAULT.submit(
            instance,
            EditBuffer.builder()
                .setBlockState(1, 70, 1, Block.STONE)
                .setBlockState(161, 70, 161, Block.STONE)
                .build(),
            UnloadedChunkPolicy.LOAD
        );
        env.tickWhile(() -> !result.isDone(), TIMEOUT);

        assertEquals(2, result.join().changed());
        assertEquals(Block.STONE, instance.getBlock(161, 70, 161));
    }

    @Test
    void failPolicyFailsBeforeQueueing(Env env) {
        Instance instance = TestInstances.flatChunk(env);

        var result = ApplyEngine.DEFAULT.submit(
            instance,
            EditBuffer.builder()
                .setBlockState(1, 70, 1, Block.STONE)
                .setBlockState(161, 70, 161, Block.STONE)
                .build()
        );

        var error = assertThrows(CompletionException.class, result::join);
        assertInstanceOf(IllegalStateException.class, error.getCause());
        env.tick();
        assertEquals(Block.AIR, instance.getBlock(1, 70, 1));
    }

    @Test
    void targetUnloadedMidApplyFailsWithWhatWasWritten(Env env) {
        Instance instance = TestInstances.flat(env);

        var result = ONE_COLUMN_PER_TICK.submit(instance, fill(0, 0, 31, 0, Block.STONE));
        env.tick(); // Writes chunk (0, 0)
        instance.unloadChunk(1, 0);
        env.tickWhile(() -> !result.isDone(), TIMEOUT);

        var error = assertThrows(CompletionException.class, result::join);
        var partial = assertInstanceOf(PartialApplyException.class, error.getCause()).partial();
        assertEquals(16, partial.changed());
        assertEquals(Block.AIR, partial.undo().getBlock(0, 70, 0));
    }

    @Test
    void closeWaitsForQueuedAppliesThenRejects(Env env) {
        Instance instance = TestInstances.flat(env);

        var queued = ONE_COLUMN_PER_TICK.submit(instance, fill(0, 0, 31, 31, Block.STONE));
        var closed = ONE_COLUMN_PER_TICK.close(instance);
        env.tickWhile(() -> !closed.isDone(), TIMEOUT);
        assertTrue(queued.isDone(), "closing lets queued applies finish");

        var rejected = ONE_COLUMN_PER_TICK.submit(instance, fill(0, 0, 0, 0, Block.GLASS));
        assertThrows(CompletionException.class, rejected::join);
        assertEquals(Block.STONE, instance.getBlock(0, 70, 0));
    }

    /// One cell per column at y 70 across the inclusive x/z box.
    private static EditBuffer fill(int minX, int minZ, int maxX, int maxZ, Block block) {
        var builder = EditBuffer.builder();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) builder.setBlockState(x, 70, z, block);
        }
        return builder.build();
    }
}
