package net.hollowcube.terraform.apply;

import net.hollowcube.terraform.TestInstances;
import net.hollowcube.terraform.buffer.BufferSection;
import net.hollowcube.terraform.buffer.EditBuffer;
import net.hollowcube.terraform.entity.EntityEdit;
import net.hollowcube.terraform.entity.TestEntityAccessor;
import net.hollowcube.terraform.util.GameDataProvider;
import net.hollowcube.terraform.util.TypedData;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@EnvTest
class ChangeTrackerTest {
    private static final ApplyEngine ENGINE = ApplyEngine.builder()
        .entities(TestEntityAccessor.INSTANCE)
        .build();

    @Test
    void drainsTheCellsAWriteChanged(Env env) {
        Instance instance = TestInstances.flat(env);
        var tracker = ChangeTracker.attach(instance, null);

        ENGINE.apply(
            instance,
            EditBuffer.builder()
                .setBlockState(1, 70, 1, Block.STONE)
                .setBlockState(1, 100, 1, Block.GLASS)
                .setBlockState(17, 70, 1, Block.STONE)
                .build()
        );
        EditBuffer drained = tracker.drain();

        assertEquals(List.of(cell(1, 70, 1), cell(17, 70, 1)), cells(drained, 0, 4, 0, 1, 4, 0));
        assertEquals(List.of(cell(1, 100, 1)), cells(drained, 0, 6, 0));
        assertEquals(Block.GLASS, drained.getBlock(1, 100, 1));
        assertTrue(tracker.isEmpty());
        assertSame(EditBuffer.empty(), tracker.drain(), "nothing left to drain");
    }

    @Test
    void cellsThatDidNotChangeAreNotMarked(Env env) {
        Instance instance = TestInstances.flatChunk(env);
        var buffer = EditBuffer.builder().setBlockState(1, 70, 1, Block.STONE).build();
        ENGINE.apply(instance, buffer);
        var tracker = ChangeTracker.attach(instance, null);

        ENGINE.apply(instance, buffer);

        assertTrue(tracker.isEmpty());
    }

    @Test
    void drainCapturesTheStateAtDrainTime(Env env) {
        Instance instance = TestInstances.flatChunk(env);
        var tracker = ChangeTracker.attach(instance, null);

        ENGINE.apply(instance, EditBuffer.builder().setBlockState(1, 70, 1, Block.STONE).build());
        ENGINE.apply(instance, EditBuffer.builder().setBlockState(1, 70, 1, Block.GLASS).build());

        assertEquals(Block.GLASS, tracker.drain().getBlock(1, 70, 1));
    }

    @Test
    void blockEntityAndBiomeChangesAreMarkedByCell(Env env) {
        Instance instance = TestInstances.flatChunk(env);
        ENGINE.apply(instance, sign(1, 70, 1, "a"));
        var tracker = ChangeTracker.attach(instance, null);

        ENGINE.apply(instance, sign(1, 70, 1, "b"));
        ENGINE.apply(
            instance,
            EditBuffer.builder().setBiome(8, 70, 8, biome("minecraft:desert")).build()
        );
        EditBuffer drained = tracker.drain();

        assertEquals(List.of(cell(1, 70, 1)), cells(drained, 0, 4, 0));
        assertEquals(instance.getBlock(1, 70, 1), drained.getBlock(1, 70, 1));
        assertEquals(biome("minecraft:desert"), drained.getBiome(8, 70, 8));
        assertEquals(EditBuffer.NONE, drained.getBiome(0, 70, 0), "only the changed biome cell");
    }

    @Test
    void wholeSectionWritesDrainAsSectionImages(Env env) {
        Instance instance = TestInstances.flatChunk(env);
        var tracker = ChangeTracker.attach(instance, null);

        ENGINE.apply(
            instance,
            EditBuffer.builder().fillSection(0, 4, 0, Block.STONE.stateId()).build()
        );
        ENGINE.apply(instance, row(86, 65));
        EditBuffer drained = tracker.drain();

        assertEquals(4096, section(drained, 0, 4, 0).count(), "a bulk write");
        assertEquals(4096, section(drained, 0, 5, 0).count(), "too many cells to list");
    }

    @Test
    void theBudgetLimitsOnlyWholeSections(Env env) {
        Instance instance = TestInstances.flatChunk(env);
        var tracker = ChangeTracker.attach(instance, null);
        var buffer = EditBuffer.builder()
            .fillSection(0, 4, 0, Block.STONE.stateId())
            .fillSection(0, 5, 0, Block.STONE.stateId())
            .setBlockState(1, 100, 1, Block.STONE)
            .build();
        ENGINE.apply(instance, buffer);

        EditBuffer first = tracker.drain(1);
        assertEquals(2, first.sectionCount(), "one whole section and every sparse one");
        assertNotNull(first.section(0, 6, 0));
        assertFalse(tracker.isEmpty());
        assertEquals(1, tracker.drain(1).sectionCount());
        assertTrue(tracker.isEmpty());
    }

    @Test
    void hostMarksAreDrained(Env env) {
        Instance instance = TestInstances.flatChunk(env);
        var tracker = ChangeTracker.attach(instance, null);

        instance.setBlock(3, 70, 3, Block.GOLD_BLOCK);
        tracker.markBlock(3, 70, 3);
        tracker.markBiome(3, 70, 3);
        EditBuffer drained = tracker.drain();

        assertEquals(Block.GOLD_BLOCK, drained.getBlock(3, 70, 3));
        assertNotEquals(EditBuffer.NONE, drained.getBiome(3, 70, 3));
    }

    @Test
    void entityChangesDrainAsTheirStateOrARemoval(Env env) {
        Instance instance = TestInstances.flatChunk(env);
        var tracker = ChangeTracker.attach(instance, TestEntityAccessor.INSTANCE);
        UUID kept = UUID.randomUUID(), removed = UUID.randomUUID();

        ENGINE.apply(
            instance,
            EditBuffer.builder()
                .addEntityEdit(new EntityEdit(kept, new Pos(1, 70, 1), zombie("a")))
                .addEntityEdit(new EntityEdit(removed, new Pos(2, 70, 2), zombie("b")))
                .build()
        );
        ENGINE.apply(
            instance,
            EditBuffer.builder()
                .addEntityEdit(EntityEdit.remove(removed, new Pos(2, 70, 2)))
                .build()
        );
        EditBuffer drained = tracker.drain();

        assertEquals(
            List.of(
                Objects.requireNonNull(TestEntityAccessor.INSTANCE.capture(instance, kept)),
                EntityEdit.remove(removed, new Pos(2, 70, 2))
            ),
            drained.entities()
        );
    }

    @Test
    void entityEditsThatThrowAreMarkedButNoOpsAreNot(Env env) {
        Instance instance = TestInstances.flatChunk(env);
        UUID broken = UUID.randomUUID(), missing = UUID.randomUUID();
        var tracker = ChangeTracker.attach(instance, TestEntityAccessor.INSTANCE);
        var engine = ApplyEngine.builder()
            .entities(new EntityApplyTest.ThrowingFor(broken))
            .build();

        engine.apply(
            instance,
            EditBuffer.builder()
                .addEntityEdit(new EntityEdit(broken, new Pos(1, 70, 1), zombie("a")))
                .addEntityEdit(EntityEdit.remove(missing, new Pos(2, 70, 2)))
                .build()
        );

        assertEquals(
            List.of(EntityEdit.remove(broken, new Pos(1, 70, 1))),
            tracker.drain().entities(),
            "the throwing edit is drained as the entity's real state, here absent"
        );
    }

    @Test
    void aColumnThatThrowsMarksEveryCellItMayHaveWritten(Env env) {
        Instance instance = TestInstances.flat(env);
        var tracker = ChangeTracker.attach(instance, null);
        var buffer = EditBuffer.builder();
        for (int x = 16; x < 26; x++) buffer.setBlockState(x, 70, 0, Block.GLASS);
        buffer.setBlockState(21, 70, 0, Block.STONE.withHandler(new ExplodingHandler()));

        assertThrows(PartialApplyException.class, () -> ENGINE.apply(instance, buffer.build()));
        EditBuffer drained = tracker.drain();

        List<Integer> expected = new ArrayList<>();
        for (int x = 16; x < 26; x++) expected.add(cell(x, 70, 0));
        assertEquals(expected, cells(drained, 1, 4, 0));
        for (int x = 16; x < 26; x++)
            assertEquals(instance.getBlock(x, 70, 0), drained.getBlock(x, 70, 0));
    }

    @Test
    void sectionsInUnloadedChunksAreDropped(Env env) {
        Instance instance = TestInstances.flat(env);
        var tracker = ChangeTracker.attach(instance, null);
        ENGINE.apply(
            instance,
            EditBuffer.builder()
                .setBlockState(1, 70, 1, Block.STONE)
                .setBlockState(17, 70, 1, Block.STONE)
                .build()
        );

        instance.unloadChunk(1, 0);
        EditBuffer drained = tracker.drain();

        assertNotNull(drained.section(0, 4, 0));
        assertNull(drained.section(1, 4, 0));
    }

    @Test
    void submittedWritesAreMarked(Env env) {
        Instance instance = TestInstances.flatChunk(env);
        var tracker = ChangeTracker.attach(instance, null);

        var result = ENGINE.submit(
            instance,
            EditBuffer.builder().setBlockState(1, 70, 1, Block.STONE).build()
        );
        env.tickWhile(() -> !result.isDone(), Duration.ofSeconds(5));

        assertEquals(Block.STONE, tracker.drain().getBlock(1, 70, 1));
    }

    @Test
    void attachIsOncePerInstanceAndDetachStopsTracking(Env env) {
        Instance instance = TestInstances.flatChunk(env);
        var tracker = ChangeTracker.attach(instance, null);
        assertThrows(IllegalStateException.class, () -> ChangeTracker.attach(instance, null));

        tracker.detach();
        ENGINE.apply(instance, EditBuffer.builder().setBlockState(1, 70, 1, Block.STONE).build());

        assertTrue(tracker.isEmpty());
        assertNotSame(tracker, ChangeTracker.attach(instance, null));
    }

    @Test
    void aDrainReplaysToTheSameWorld(Env env) {
        Instance source = TestInstances.flatChunk(env);
        var tracker = ChangeTracker.attach(source, null);
        var edits = EditBuffer.builder()
            .fillSection(0, 4, 0, Block.STONE.stateId())
            .setBlockState(2, 87, 3, Block.OAK_STAIRS.withProperty("facing", "east"));
        for (int x = 0; x < 16; x += 4) edits.setBiome(x, 64, 0, biome("minecraft:desert"));
        ENGINE.apply(source, edits.build());
        ENGINE.apply(source, sign(5, 72, 5, "kept"));

        var type = EditBuffer.serializer(GameDataProvider.NOOP, MinecraftServer.getBiomeRegistry());
        EditBuffer drained = tracker.drain();
        byte[] bytes = NetworkBuffer.makeArray(out -> out.write(type, drained));
        EditBuffer read = NetworkBuffer.wrap(bytes, 0, bytes.length).read(type);

        Instance target = TestInstances.flatChunk(env);
        ENGINE.apply(target, EditBuffer.builder().setBlockState(9, 75, 9, Block.GLASS).build());
        ENGINE.apply(target, read);

        for (int y = 64; y < 96; y++)
            for (int z = 0; z < 16; z++)
                for (int x = 0; x < 16; x++)
                    assertEquals(
                        source.getBlock(x, y, z),
                        target.getBlock(x, y, z),
                        x + "," + y + "," + z
                    );
        var sourceBiomes = Objects.requireNonNull(source.getChunk(0, 0))
            .getSection(4)
            .biomePalette();
        var targetBiomes = Objects.requireNonNull(target.getChunk(0, 0))
            .getSection(4)
            .biomePalette();
        for (int y = 0; y < 4; y++)
            for (int z = 0; z < 4; z++)
                for (int x = 0; x < 4; x++)
                    assertEquals(sourceBiomes.get(x, y, z), targetBiomes.get(x, y, z));
    }

    /// The set block cells of the given sections (section coordinate triples), as section-local indices.
    private static List<Integer> cells(EditBuffer buffer, int... sectionCoords) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < sectionCoords.length; i += 3) {
            section(
                buffer,
                sectionCoords[i],
                sectionCoords[i + 1],
                sectionCoords[i + 2]
            ).forEachBlockState((x, y, z, _) -> out.add(y << 8 | z << 4 | x));
        }
        out.sort(null);
        return out;
    }

    private static BufferSection section(EditBuffer buffer, int sx, int sy, int sz) {
        return Objects.requireNonNull(
            buffer.section(sx, sy, sz),
            "section " + sx + "," + sy + "," + sz
        );
    }

    private static int cell(int x, int y, int z) {
        return (y & 15) << 8 | (z & 15) << 4 | (x & 15);
    }

    /// `count` stone cells in the section at block y `y`, along x then z from the chunk's corner.
    private static EditBuffer row(int y, int count) {
        var out = EditBuffer.builder();
        for (int i = 0; i < count; i++) out.setBlockState(i & 15, y, i >> 4, Block.STONE);
        return out.build();
    }

    private static EditBuffer sign(int x, int y, int z, String text) {
        var nbt = CompoundBinaryTag.builder().putString("Text", text).build();
        return EditBuffer.builder().setBlockState(x, y, z, Block.OAK_SIGN.withNbt(nbt)).build();
    }

    private static TypedData zombie(String value) {
        return new TypedData(
            EntityType.ZOMBIE.name(),
            CompoundBinaryTag.builder().putString("value", value).build()
        );
    }

    private static int biome(String key) {
        var registry = MinecraftServer.getBiomeRegistry();
        return registry.getId(registry.getKey(Key.key(key)));
    }

    /// Fails the apply part way through a column: the engine asks after writing the cell's palette and entry.
    private static final class ExplodingHandler implements BlockHandler {
        @Override
        public Key getKey() {
            return Key.key("test:exploding");
        }

        @Override
        public boolean isTickable() {
            throw new IllegalStateException("exploding handler");
        }
    }
}
