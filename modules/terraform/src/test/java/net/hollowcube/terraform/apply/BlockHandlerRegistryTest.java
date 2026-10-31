package net.hollowcube.terraform.apply;

import net.hollowcube.terraform.TestInstances;
import net.hollowcube.terraform.buffer.EditBuffer;
import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@EnvTest
class BlockHandlerRegistryTest {

    /// Minimal named handler. Identity-compared in assertions (handlers are singletons).
    private record NamedHandler(Key getKey) implements BlockHandler {}

    @Test
    void registryBlockEntityWithoutHostEntryGetsABareEntry(Env env) {
        // BlockManager registrations are keyed by handler key, not by block, so apply never consults them.
        MinecraftServer.getBlockManager().registerHandler(
            Block.LECTERN.key(),
            () -> new NamedHandler(Block.LECTERN.key())
        );
        Instance instance = TestInstances.flatChunk(env);

        ApplyEngine.DEFAULT.apply(
            instance,
            EditBuffer.builder().setBlockState(1, 60, 1, Block.LECTERN).build()
        );

        // Throws when the cell has no chunk entry.
        var placed = instance.getBlock(1, 60, 1, Block.Getter.Condition.CACHED);
        assertNull(placed.handler());
    }

    @Test
    void hostEntryAttachesToPlainBlockPerCell(Env env) {
        var handler = new NamedHandler(Block.SEA_LANTERN.key());
        var handlers = Map.of(Block.SEA_LANTERN.key(), handler);
        var engine = ApplyEngine.builder().blockHandlers(handlers).build();
        Instance instance = TestInstances.flatChunk(env);

        engine.apply(
            instance,
            EditBuffer.builder().setBlockState(1, 60, 1, Block.SEA_LANTERN).build()
        );

        assertSame(handler, instance.getBlock(1, 60, 1).handler());
        // An unmapped handlers: the same block applies bare.
        ApplyEngine.DEFAULT.apply(
            instance,
            EditBuffer.builder().setBlockState(2, 60, 2, Block.SEA_LANTERN).build()
        );
        assertNull(instance.getBlock(2, 60, 2).handler());
    }

    @Test
    void hostEntryAttachesThroughUniformFill(Env env) {
        var handler = new NamedHandler(Block.SEA_LANTERN.key());
        var handlers = Map.of(Block.SEA_LANTERN.key(), handler);
        var engine = ApplyEngine.builder().blockHandlers(handlers).build();
        Instance instance = TestInstances.flatChunk(env);

        // A uniform mapped fill must abandon the Palette.fill fast path (every cell needs an entry).
        engine.apply(
            instance,
            EditBuffer.builder().fillSection(0, 3, 0, Block.SEA_LANTERN.stateId()).build()
        );

        assertSame(handler, instance.getBlock(0, 48, 0).handler());
        assertSame(handler, instance.getBlock(15, 63, 15).handler());
    }

    @Test
    void hostEntryAttachesThroughFullCover(Env env) {
        var handler = new NamedHandler(Block.SEA_LANTERN.key());
        var handlers = Map.of(Block.SEA_LANTERN.key(), handler);
        var engine = ApplyEngine.builder().blockHandlers(handlers).build();
        Instance instance = TestInstances.flatChunk(env);

        // Full-cover non-uniform section (two blocks) → bulk copyFrom + the hasCustom post-pass.
        var builder = EditBuffer.builder();
        for (int x = 0; x < 16; x++)
            for (int y = 48; y < 64; y++)
                for (int z = 0; z < 16; z++)
                    builder.setBlockState(
                        x,
                        y,
                        z,
                        (x + y + z) % 2 == 0 ? Block.SEA_LANTERN : Block.STONE
                    );
        engine.apply(instance, builder.build());

        assertSame(handler, instance.getBlock(0, 48, 0).handler()); // sum even → mapped block
        assertEquals(Block.STONE, instance.getBlock(1, 48, 0)); // unmapped stays bare
        assertNull(instance.getBlock(1, 48, 0).handler());
    }

    @Test
    void carriedHandlerIsNeverOverridden(Env env) {
        var carried = new NamedHandler(Key.key("test:carried"));
        var mapped = new NamedHandler(Block.SEA_LANTERN.key());
        var handlers = Map.of(Block.SEA_LANTERN.key(), mapped);
        var engine = ApplyEngine.builder().blockHandlers(handlers).build();
        Instance instance = TestInstances.flatChunk(env);

        engine.apply(
            instance,
            EditBuffer.builder()
                .setBlockState(1, 60, 1, Block.SEA_LANTERN.withHandler(carried))
                .build()
        );

        assertSame(carried, instance.getBlock(1, 60, 1).handler());
    }

    @Test
    void undoRestoresPreAttachWorld(Env env) {
        var handler = new NamedHandler(Block.SEA_LANTERN.key());
        var handlers = Map.of(Block.SEA_LANTERN.key(), handler);
        var engine = ApplyEngine.builder().blockHandlers(handlers).build();
        Instance instance = TestInstances.flatChunk(env);
        Block original = instance.getBlock(1, 60, 1);

        ApplyEngine.ApplyResult result = engine.apply(
            instance,
            EditBuffer.builder().setBlockState(1, 60, 1, Block.SEA_LANTERN).build()
        );
        assertSame(handler, instance.getBlock(1, 60, 1).handler());

        engine.apply(instance, result.undo());
        assertEquals(original, instance.getBlock(1, 60, 1));
    }

    @Test
    void unknownBlockKeyThrowsAtConstruction() {
        var handler = new NamedHandler(Key.key("test:nope"));
        assertThrows(
            IllegalArgumentException.class,
            () -> ApplyEngine.builder().blockHandlers(Map.of(Key.key("test:not_a_block"), handler))
        );
    }

    /// Identity-equals handler, so two instances are different objects of the same handler type.
    private static final class FreshHandler implements BlockHandler {
        @Override
        public Key getKey() {
            return Block.BARREL.key();
        }
    }

    @Test
    void reapplyingIdenticalContentIsANoOp(Env env) {
        var engine = ApplyEngine.builder()
            .blockHandlers(Map.of(Block.BARREL.key(), new FreshHandler()))
            .build();
        Instance instance = TestInstances.flatChunk(env);
        EditBuffer edit = EditBuffer.builder().setBlockState(1, 60, 1, Block.BARREL).build();

        var first = engine.apply(instance, edit);
        var second = engine.apply(instance, edit);

        assertEquals(1, first.changed());
        assertEquals(0, second.changed(), "identical re-apply must be a clean skip");
    }

    @Test
    void reapplyingOverALoadedBlockIsANoOp(Env env) {
        // A block from a loader or setBlock carries its own handler instance, never the host map's.
        var engine = ApplyEngine.builder()
            .blockHandlers(Map.of(Block.BARREL.key(), new FreshHandler()))
            .build();
        Instance instance = TestInstances.flatChunk(env);
        var loaded = new FreshHandler();
        instance.setBlock(1, 60, 1, Block.BARREL.withHandler(loaded));

        var result = engine.apply(
            instance,
            EditBuffer.builder().setBlockState(1, 60, 1, Block.BARREL).build()
        );

        assertEquals(0, result.changed());
        assertSame(loaded, instance.getBlock(1, 60, 1).handler(), "the loaded handler stays");
    }
}
