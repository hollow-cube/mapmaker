package dev.hollowcube.replay;

import dev.hollowcube.replay.data.ChunkIndex;
import dev.hollowcube.replay.data.ReplayHeader;
import dev.hollowcube.replay.data.ReplayPreamble;
import dev.hollowcube.replay.event.DestroyEntityEvent;
import dev.hollowcube.replay.event.ReplayEvents;
import dev.hollowcube.replay.io.SegmentedReplayCommit;
import dev.hollowcube.replay.io.SegmentedReplayWriter;
import net.minestom.server.network.NetworkBuffer;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

final class ReplayRecorderTest {

    @Test
    void closeCommitsCompletedTicksAndClosesWriterInOrder() {
        var writer = new TestWriter();
        var recorder = newRecorder(writer);

        recorder.advance();
        recorder.advance();

        var close = recorder.close();
        assertSame(close, recorder.close());
        close.join();

        assertEquals(List.of("commit:0", "close"), writer.operations);

        var commit = writer.commits.getFirst();
        assertFalse(commit.finished());
        assertTrue(commit.segment().length > 0);
        assertEquals(2, headerOf(commit).tickCount());
        assertEquals(1, headerOf(commit).chunkCount());
    }

    @Test
    void flushCommitsCompletedTicksAndRecordingCanContinue() {
        var writer = new TestWriter();
        var recorder = newRecorder(writer);

        recorder.advance();
        recorder.flush().join();
        recorder.advance();
        recorder.close().join();

        assertEquals(List.of("commit:0", "commit:1", "close"), writer.operations);

        var last = writer.commits.getLast();
        assertEquals(2, headerOf(last).tickCount());
        assertEquals(2, headerOf(last).chunkCount());
    }

    @Test
    void finishMarksTheLastCommitFinal() {
        var writer = new TestWriter();
        var recorder = newRecorder(writer);

        recorder.advance();
        recorder.finish().join();

        assertEquals(List.of("commit:0", "close"), writer.operations);
        assertTrue(writer.commits.getFirst().finished());
    }

    @Test
    void finishAfterEverythingIsCommittedStillEmitsAFinalCommit() {
        var writer = new TestWriter();
        var recorder = newRecorder(writer);

        recorder.advance();
        recorder.flush().join();
        recorder.finish().join();

        assertEquals(List.of("commit:0", "commit", "close"), writer.operations);

        var last = writer.commits.getLast();
        assertTrue(last.finished());
        assertNull(last.segmentIndex());
        assertEquals(0, last.segment().length);
    }

    @Test
    void finishingARecordingThatNeverCommittedDoesNotCreateOne() {
        var writer = new TestWriter();
        var recorder = newRecorder(writer);

        recorder.finish().join();

        assertEquals(List.of("close"), writer.operations);
    }

    @Test
    void closeStillClosesWriterWhenACommitFails() {
        var writer = new TestWriter();
        writer.failCommits = true;
        var recorder = newRecorder(writer);
        recorder.advance();

        assertThrows(CompletionException.class, () -> recorder.close().join());
        assertTrue(writer.operations.contains("close"));
    }

    @Test
    void aFailedCommitStopsTheRecorderInsteadOfBufferingTicksItCannotPersist() {
        var writer = new TestWriter();
        writer.failCommits = true;
        var recorder = newRecorder(writer);

        recorder.advance();
        recorder.flush().handle((_, _) -> null).join();
        assertNotNull(recorder.failure());

        var tick = recorder.tick();
        recorder.advance();
        recorder.advance();
        assertEquals(tick, recorder.tick());
        assertEquals(1, writer.commits.size());
    }

    @Test
    void everyChunkOpensWithASnapshotAndSaysSoInTheIndex() {
        var writer = new TestWriter();
        var snapshotTicks = new ArrayList<Integer>();
        var holder = new AtomicReference<ReplayRecorder>();
        holder.set(newRecorder(writer, () -> snapshotTicks.add(holder.get().tick())));
        var recorder = holder.get();

        // Enough ticks to cross the recorder's chunk length several times.
        for (var tick = 0; tick < 250; tick++) {
            recorder.submit(new DestroyEntityEvent(1));
            recorder.advance();
        }
        recorder.finish().join();

        var index = ReplayPreamble.read(writer.commits.getLast().preamble()).index();
        assertEquals(3, index.size());
        assertTrue(index.stream().allMatch(ChunkIndex::hasSnapshot));
        assertEquals(index.stream().map(ChunkIndex::startTick).toList(), snapshotTicks);
    }

    @Test
    void aChunkClosedByItsByteLimitStillOpensTheNextOneWithASnapshot() {
        var writer = new TestWriter();
        var snapshotTicks = new ArrayList<Integer>();
        var holder = new AtomicReference<ReplayRecorder>();
        holder.set(newRecorder(writer, () -> snapshotTicks.add(holder.get().tick())));
        var recorder = holder.get();

        // Far short of the chunk tick limit, but well past its byte limit.
        for (var tick = 0; tick < 20; tick++) {
            for (var i = 0; i < 30000; i++)
                recorder.submit(new DestroyEntityEvent(i));
            recorder.advance();
        }
        recorder.finish().join();

        var index = ReplayPreamble.read(writer.commits.getLast().preamble()).index();
        assertTrue(index.size() > 1, "expected the byte limit to close a chunk early");
        assertTrue(index.getFirst().tickCount() < 100);
        assertTrue(index.stream().allMatch(ChunkIndex::hasSnapshot));
        assertEquals(index.stream().map(ChunkIndex::startTick).toList(), snapshotTicks);
    }

    @Test
    void flushingClosesAChunkAndTheNextOneIsSnapshottedAgain() {
        var writer = new TestWriter();
        var snapshots = new ArrayList<Integer>();
        var holder = new AtomicReference<ReplayRecorder>();
        holder.set(newRecorder(writer, () -> snapshots.add(holder.get().tick())));
        var recorder = holder.get();

        recorder.advance();
        recorder.flush().join();
        recorder.advance();
        recorder.finish().join();

        assertEquals(List.of(0, 1), snapshots);
        assertTrue(ReplayPreamble.read(writer.commits.getLast().preamble()).index()
            .stream().allMatch(ChunkIndex::hasSnapshot));
    }

    @Test
    void terminatingKeepsEventsSubmittedAfterTheLastAdvance() {
        var writer = new TestWriter();
        var recorder = newRecorder(writer);

        recorder.advance();
        // A run-ending event is submitted into the open tick and terminated on, never advanced.
        recorder.submit(new DestroyEntityEvent(1));
        recorder.finish().join();

        var commit = writer.commits.getLast();
        assertTrue(commit.finished());
        assertEquals(2, headerOf(commit).tickCount());
        assertEquals(1, recorder.stats().events());
    }

    @Test
    void statsCountEventsForTheWholeRecordingAndForTheOpenChunk() {
        var recorder = newRecorder(new TestWriter());

        // Past the chunk tick limit, so one chunk is closed and the rest are still open.
        for (var tick = 0; tick < 150; tick++) {
            recorder.submit(new DestroyEntityEvent(tick));
            recorder.advance();
        }

        var stats = recorder.stats();
        assertEquals(150, stats.tick());
        assertEquals(1, stats.chunks());
        assertEquals(150, stats.events());
        assertEquals(50, stats.chunkEvents());
        assertTrue(stats.bytes() > 0);
    }

    @Test
    void statsMeasureTheCompressedChunksAndForgetTheDiscardedOpenTick() {
        var writer = new TestWriter();
        var recorder = newRecorder(writer);

        recorder.submit(new DestroyEntityEvent(1));
        recorder.advance();
        recorder.submit(new DestroyEntityEvent(2)); // never advanced, so never recorded
        recorder.flush().join();

        var stats = recorder.stats();
        assertEquals(1, stats.events());
        assertEquals(0, stats.chunkEvents());
        assertEquals(1, stats.chunks());

        var index = ReplayPreamble.read(writer.commits.getLast().preamble()).index();
        assertEquals(index.stream().mapToLong(ChunkIndex::compressedLength).sum(), stats.bytes());
    }

    private static ReplayRecorder newRecorder(SegmentedReplayWriter writer) {
        return newRecorder(writer, () -> {
        });
    }

    private static ReplayRecorder newRecorder(SegmentedReplayWriter writer, Runnable snapshot) {
        return ReplayRecorder.create(
            ReplayEvents.builder().build(),
            writer,
            UUID.randomUUID(),
            ReplayHeader.worldVersion(UUID.randomUUID()),
            snapshot
        );
    }

    private static ReplayHeader headerOf(SegmentedReplayCommit commit) {
        var preamble = MemorySegment.ofArray(commit.preamble());
        return new ReplayHeader(NetworkBuffer.wrap(preamble, 0, preamble.byteSize()));
    }

    private static final class TestWriter implements SegmentedReplayWriter {
        private final List<String> operations = new ArrayList<>();
        private final List<SegmentedReplayCommit> commits = new ArrayList<>();

        private boolean failCommits;

        @Override
        public void commit(SegmentedReplayCommit commit) {
            operations.add(commit.hasSegment() ? "commit:" + commit.segmentIndex() : "commit");
            commits.add(commit);
            if (failCommits) throw new IllegalStateException("commit failed");
        }

        @Override
        public void close() {
            operations.add("close");
        }
    }
}
