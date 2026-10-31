package net.hollowcube.terraform.apply;

import net.hollowcube.terraform.TestInstances;
import net.hollowcube.terraform.buffer.EditBuffer;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@EnvTest
class BlockHandlerLifecycleTest {

    private record Event(String kind, Point pos, Block block, Block other) {}

    /// Records onPlace/onDestroy invocations. `other` is Placement#previousBlock / Destroy#newBlock.
    private static final class RecordingHandler implements BlockHandler {
        final List<Event> events = new ArrayList<>();

        @Override
        public Key getKey() {
            return Key.key("test:recording");
        }

        @Override
        public void onPlace(Placement placement) {
            events.add(
                new Event(
                    "place",
                    placement.getBlockPosition(),
                    placement.getBlock(),
                    placement.getPreviousBlock()
                )
            );
        }

        @Override
        public void onDestroy(Destroy destroy) {
            events.add(
                new Event(
                    "destroy",
                    destroy.getBlockPosition(),
                    destroy.getBlock(),
                    destroy.getNewBlock()
                )
            );
        }
    }

    private static ApplyEngine.ApplyResult apply(
        Instance instance,
        Map<Key, ? extends BlockHandler> handlers,
        EditBuffer buffer
    ) {
        return ApplyEngine.builder().blockHandlers(handlers).build().apply(instance, buffer);
    }

    @Test
    void placeFiresOnPlaceWithPositionAndPrevious(Env env) {
        var handler = new RecordingHandler();
        var handlers = Map.of(Block.SEA_LANTERN.key(), handler);
        Instance instance = TestInstances.flatChunk(env);
        Block previous = instance.getBlock(1, 60, 1);

        apply(
            instance,
            handlers,
            EditBuffer.builder().setBlockState(1, 60, 1, Block.SEA_LANTERN).build()
        );

        assertEquals(1, handler.events.size());
        Event event = handler.events.getFirst();
        assertEquals("place", event.kind());
        assertEquals(new Vec(1, 60, 1), event.pos());
        assertEquals(Block.SEA_LANTERN.stateId(), event.block().stateId());
        assertEquals(previous, event.other());
    }

    @Test
    void overwriteFiresDestroyThenPlace(Env env) {
        var handler = new RecordingHandler();
        var handlers = Map.of(Block.SEA_LANTERN.key(), handler, Block.GLOWSTONE.key(), handler);
        Instance instance = TestInstances.flatChunk(env);
        apply(
            instance,
            handlers,
            EditBuffer.builder().setBlockState(1, 60, 1, Block.SEA_LANTERN).build()
        );
        handler.events.clear();

        apply(
            instance,
            handlers,
            EditBuffer.builder().setBlockState(1, 60, 1, Block.GLOWSTONE).build()
        );

        assertEquals(
            List.of("destroy", "place"),
            handler.events.stream().map(Event::kind).toList()
        );
        Event destroy = handler.events.getFirst();
        assertEquals(Block.SEA_LANTERN.stateId(), destroy.block().stateId());
        assertEquals(Block.GLOWSTONE.stateId(), destroy.other().stateId()); // Destroy#newBlock
    }

    @Test
    void overwriteWithPlainFiresDestroyOnly(Env env) {
        var handler = new RecordingHandler();
        var handlers = Map.of(Block.SEA_LANTERN.key(), handler);
        Instance instance = TestInstances.flatChunk(env);
        apply(
            instance,
            handlers,
            EditBuffer.builder().setBlockState(1, 60, 1, Block.SEA_LANTERN).build()
        );
        handler.events.clear();

        apply(
            instance,
            handlers,
            EditBuffer.builder().setBlockState(1, 60, 1, Block.STONE).build()
        );

        assertEquals(1, handler.events.size());
        assertEquals("destroy", handler.events.getFirst().kind());
    }

    @Test
    void uniformFillFiresDestroyForWipedEntries(Env env) {
        var handler = new RecordingHandler();
        var handlers = Map.of(Block.SEA_LANTERN.key(), handler);
        Instance instance = TestInstances.flatChunk(env);
        apply(
            instance,
            handlers,
            EditBuffer.builder()
                .setBlockState(1, 50, 1, Block.SEA_LANTERN)
                .setBlockState(2, 51, 2, Block.SEA_LANTERN)
                .build()
        );
        handler.events.clear();

        // Plain uniform fill over section y=3 (blocks 48..63) → Palette.fill fast path + entry wipe.
        apply(
            instance,
            handlers,
            EditBuffer.builder().fillSection(0, 3, 0, Block.STONE.stateId()).build()
        );

        assertEquals(2, handler.events.size());
        assertTrue(handler.events.stream().allMatch(e -> e.kind().equals("destroy")));
        assertTrue(handler.events.stream().allMatch(e -> e.other().equals(Block.STONE)));
    }

    @Test
    void fullCoverFiresDestroyAndPlace(Env env) {
        var handler = new RecordingHandler();
        var handlers = Map.of(Block.SEA_LANTERN.key(), handler);
        Instance instance = TestInstances.flatChunk(env);
        apply(
            instance,
            handlers,
            EditBuffer.builder().setBlockState(1, 50, 1, Block.SEA_LANTERN).build()
        );
        handler.events.clear();

        // Full-cover non-uniform section overwrite: destroy for the wiped entry, place per mapped cell.
        var builder = EditBuffer.builder();
        for (int x = 0; x < 16; x++)
            for (int y = 48; y < 64; y++)
                for (int z = 0; z < 16; z++)
                    builder.setBlockState(
                        x,
                        y,
                        z,
                        x == 0 && y == 48 && z == 0 ? Block.SEA_LANTERN : Block.STONE
                    );
        apply(instance, handlers, builder.build());

        assertEquals(
            List.of("destroy", "place"),
            handler.events.stream().map(Event::kind).toList()
        );
        assertEquals(new Vec(1, 50, 1), handler.events.getFirst().pos());
        assertEquals(new Vec(0, 48, 0), handler.events.getLast().pos());
    }

    @Test
    void fullCoverSideMapCellFiresOnce(Env env) {
        var handler = new RecordingHandler();
        var handlers = Map.of(Block.SEA_LANTERN.key(), handler);
        Instance instance = TestInstances.flatChunk(env);

        // A mapped cell that is ALSO side-map-carried (e.g. a replayed undo capture): the hasCustom
        // palette pass must leave it to the side-map pass, one onPlace, not two.
        var builder = EditBuffer.builder();
        for (int x = 0; x < 16; x++)
            for (int y = 48; y < 64; y++)
                for (int z = 0; z < 16; z++) builder.setBlockState(x, y, z, Block.STONE);
        builder.setBlockState(0, 48, 0, Block.SEA_LANTERN.withHandler(handler));
        apply(instance, handlers, builder.build());

        assertEquals(1, handler.events.size());
        assertEquals("place", handler.events.getFirst().kind());
        assertEquals(new Vec(0, 48, 0), handler.events.getFirst().pos());
    }

    @Test
    void identicalFullCoverReapplyFiresNothing(Env env) {
        var handler = new RecordingHandler();
        var handlers = Map.of(Block.SEA_LANTERN.key(), handler);
        Instance instance = TestInstances.flatChunk(env);
        var builder = EditBuffer.builder();
        for (int x = 0; x < 16; x++)
            for (int y = 48; y < 64; y++)
                for (int z = 0; z < 16; z++)
                    builder.setBlockState(
                        x,
                        y,
                        z,
                        (x + z) % 2 == 0 ? Block.SEA_LANTERN : Block.STONE
                    );
        // A side-map cell carrying data, beside the mapped plain cells.
        builder.setBlockState(
            0,
            48,
            0,
            Block.SEA_LANTERN.withHandler(handler)
                .withNbt(CompoundBinaryTag.builder().putInt("a", 1).build())
        );
        var buffer = builder.build();
        apply(instance, handlers, buffer);
        handler.events.clear();

        var second = apply(instance, handlers, buffer);

        assertEquals(0, second.changed());
        assertTrue(handler.events.isEmpty(), "unchanged cells fire nothing: " + handler.events);
    }

    @Test
    void throwingHandlerDoesNotVoidTheEdit(Env env) {
        var throwing = new BlockHandler() {
            @Override
            public Key getKey() {
                return Key.key("test:throwing");
            }

            @Override
            public void onPlace(Placement placement) {
                throw new IllegalStateException("host bug");
            }
        };
        var handlers = Map.of(Block.SEA_LANTERN.key(), throwing);
        Instance instance = TestInstances.flatChunk(env);

        ApplyEngine.ApplyResult result = apply(
            instance,
            handlers,
            EditBuffer.builder()
                .setBlockState(1, 60, 1, Block.SEA_LANTERN)
                .setBlockState(2, 60, 2, Block.SEA_LANTERN)
                .setBlockState(3, 60, 3, Block.STONE)
                .build()
        );

        assertEquals(3, result.changed());
        assertSame(throwing, instance.getBlock(1, 60, 1).handler());
        assertSame(throwing, instance.getBlock(2, 60, 2).handler());
        assertEquals(Block.STONE, instance.getBlock(3, 60, 3));
    }
}
