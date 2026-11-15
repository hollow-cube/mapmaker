package net.hollowcube.mapmaker.map.wal;

import net.hollowcube.mapmaker.map.entity.PhysicsTestEnv;
import net.hollowcube.mapmaker.map.instance.MapInstance;
import net.hollowcube.terraform.apply.ApplyEngine;
import net.hollowcube.terraform.buffer.EditBuffer;
import net.hollowcube.terraform.util.GameDataProvider;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.world.DimensionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class WalWriterTest {
    private static final long HEAD_SEQ = 5;
    private static NetworkBuffer.Type<EditBuffer> edits;

    private final List<MapInstance> instances = new ArrayList<>();
    private final List<WalWriter.Batch> delivered = new CopyOnWriteArrayList<>();
    private final AtomicInteger gaps = new AtomicInteger();
    private volatile boolean failing;

    @BeforeAll
    static void init() {
        PhysicsTestEnv.init();
        edits = EditBuffer.serializer(GameDataProvider.NOOP, MinecraftServer.getBiomeRegistry());
    }

    @AfterEach
    void cleanup() {
        instances.forEach(MapInstance::unload);
    }

    @Test
    void numbersBatchesFromTheHeadInOrder() {
        var instance = createInstance(1);
        var writer = writer(instance);

        instance.setBlock(1, 41, 2, Block.STONE);
        assertEquals(HEAD_SEQ + 1, writer.flush());
        instance.setBlock(1, 42, 2, Block.DIRT);
        assertEquals(HEAD_SEQ + 2, writer.flush());
        writer.close();

        assertEquals(2, delivered.size());
        assertEquals(HEAD_SEQ + 1, delivered.get(0).firstSeq());
        assertEquals(HEAD_SEQ + 2, delivered.get(1).firstSeq());
        assertEquals(Block.STONE.stateId(), edit(delivered.get(0)).getBlockState(1, 41, 2));
        assertEquals(EditBuffer.NONE, edit(delivered.get(0)).getBlockState(1, 42, 2));
        assertEquals(Block.DIRT.stateId(), edit(delivered.get(1)).getBlockState(1, 42, 2));
    }

    @Test
    void nothingChangedEmitsNothing() {
        var writer = writer(createInstance(1));
        assertEquals(HEAD_SEQ, writer.flush());
        assertEquals(HEAD_SEQ, writer.flush());
        writer.close();
        assertTrue(delivered.isEmpty());
    }

    @Test
    void capturesTheStateAtFlushNotEachWrite() {
        var instance = createInstance(1);
        var writer = writer(instance);
        instance.setBlock(1, 41, 2, Block.STONE);
        instance.setBlock(1, 41, 2, Block.DIRT);
        writer.close();

        assertEquals(1, delivered.size());
        assertEquals(Block.DIRT.stateId(), edit(delivered.getFirst()).getBlockState(1, 41, 2));
    }

    @Test
    void worldTagSetsShareTheNextBatch() {
        var instance = createInstance(1);
        var writer = writer(instance);
        writer.setWorldTag(4000, "spawn", StringBinaryTag.stringBinaryTag("a"));
        instance.setBlock(1, 41, 2, Block.STONE);
        assertEquals(HEAD_SEQ + 2, writer.flush());
        writer.close();

        var batch = decode(delivered.getFirst());
        assertEquals(new WalRecord.WorldTag(batch.records().getFirst().time(), 4000, "spawn", StringBinaryTag.stringBinaryTag("a")),
            batch.records().get(0));
        assertInstanceOf(WalRecord.Edit.class, batch.records().get(1));
        assertEquals(HEAD_SEQ + 2, delivered.getFirst().lastSeq());
    }

    @Test
    void aChangeLargerThanOneTickIsOneBatch() throws Exception {
        // 24 sections a chunk: three ticks' worth and a part of a fourth
        int chunks = WalWriter.SECTION_BUDGET * 3 / 24 + 1;
        int total = chunks * 24;
        var instance = createInstance(chunks);
        var writer = writer(instance);
        writer.start();
        var box = Area.cuboid(new BlockVec(0, -64, 0), new BlockVec(chunks * 16 - 1, 319, 15));
        ApplyEngine.DEFAULT.apply(instance, EditBuffer.builder().fill(box, Block.STONE.stateId()).build());

        for (int i = 0; i < 400 && delivered.isEmpty(); i++) {
            instance.scheduler().processTick();
            instance.scheduler().processTickEnd();
            Thread.sleep(5);
        }
        writer.close();

        assertEquals(1, delivered.size());
        var records = decode(delivered.getFirst()).records();
        assertEquals(4, records.size());
        assertEquals(HEAD_SEQ + 1, delivered.getFirst().firstSeq());
        assertEquals(HEAD_SEQ + 4, delivered.getFirst().lastSeq());
        int sections = 0;
        for (var record : records) {
            int count = ((WalRecord.Edit) record).buffer().sectionCount();
            assertTrue(count <= WalWriter.SECTION_BUDGET);
            sections += count;
        }
        assertEquals(total, sections);
    }

    @Test
    void aBatchIsCutBeforeItHoldsTooMuch() throws Exception {
        int chunks = (WalWriter.BATCH_SECTIONS + WalWriter.SECTION_BUDGET) / 24 + 1;
        var instance = createInstance(chunks);
        var writer = writer(instance);
        writer.start();
        var box = Area.cuboid(new BlockVec(0, -64, 0), new BlockVec(chunks * 16 - 1, 319, 15));
        ApplyEngine.DEFAULT.apply(instance, EditBuffer.builder().fill(box, Block.STONE.stateId()).build());

        for (int i = 0; i < 400 && delivered.size() < 2; i++) {
            instance.scheduler().processTick();
            instance.scheduler().processTickEnd();
            Thread.sleep(5);
        }
        writer.close();

        assertEquals(2, delivered.size());
        int first = 0;
        for (var record : decode(delivered.getFirst()).records())
            first += ((WalRecord.Edit) record).buffer().sectionCount();
        assertEquals(WalWriter.BATCH_SECTIONS, first);
        assertEquals(delivered.getFirst().lastSeq() + 1, delivered.get(1).firstSeq());
    }

    @Test
    void anUndeliveredBatchDropsWhatIsQueuedAndReportsTheGapTwice() throws Exception {
        var instance = createInstance(1);
        var writer = writer(instance);

        failing = true;
        instance.setBlock(1, 41, 2, Block.STONE);
        writer.flush();
        awaitGaps(1);
        assertTrue(delivered.isEmpty());

        failing = false;
        instance.setBlock(1, 42, 2, Block.DIRT);
        assertEquals(HEAD_SEQ + 2, writer.flush());
        writer.close();

        assertEquals(2, gaps.get());
        assertEquals(1, delivered.size());
        assertEquals(HEAD_SEQ + 2, delivered.getFirst().firstSeq());
    }

    @Test
    void stopSendsNothingMore() {
        var instance = createInstance(1);
        var writer = writer(instance);
        instance.setBlock(1, 41, 2, Block.STONE);
        writer.stop();
        assertEquals(HEAD_SEQ, writer.flush());
        assertTrue(delivered.isEmpty());

        var again = writer(instance);
        instance.setBlock(1, 42, 2, Block.STONE);
        again.close();
        assertEquals(1, delivered.size());
        assertEquals(EditBuffer.NONE, edit(delivered.getFirst()).getBlockState(1, 41, 2));
        assertEquals(Block.STONE.stateId(), edit(delivered.getFirst()).getBlockState(1, 42, 2));
    }

    private WalWriter writer(MapInstance instance) {
        return new WalWriter(instance, null, edits, HEAD_SEQ, batch -> {
            if (failing) throw new IllegalStateException("not delivered");
            delivered.add(batch);
        }, gaps::incrementAndGet);
    }

    private void awaitGaps(int count) throws InterruptedException {
        for (int i = 0; i < 400 && gaps.get() < count; i++) Thread.sleep(5);
        assertEquals(count, gaps.get());
    }

    private int sections() {
        int total = 0;
        for (var batch : delivered) total += edit(batch).sectionCount();
        return total;
    }

    private static WalBatch decode(WalWriter.Batch batch) {
        return WalBatch.decode(NetworkBuffer.wrap(batch.bytes(), 0, batch.bytes().length), edits);
    }

    private static EditBuffer edit(WalWriter.Batch batch) {
        for (var record : decode(batch).records()) {
            if (record instanceof WalRecord.Edit edit) return edit.buffer();
        }
        throw new AssertionError("no edit record");
    }

    private MapInstance createInstance(int chunks) {
        var instance = new MapInstance("test", DimensionType.OVERWORLD, MapInstance.LightingMode.LOADED);
        instances.add(instance);
        for (int x = 0; x < chunks; x++) instance.loadChunk(x, 0).join();
        return instance;
    }
}
