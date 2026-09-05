package dev.hollowcube.replay;

import dev.hollowcube.replay.data.ChunkIndex;
import dev.hollowcube.replay.data.ReplayHeader;
import dev.hollowcube.replay.event.AbsoluteMoveEvent;
import dev.hollowcube.replay.event.ReplayEvent;
import dev.hollowcube.replay.event.ReplayEvents;
import dev.hollowcube.replay.io.CompactedReplayReader;
import dev.hollowcube.replay.io.RunOutcome;
import dev.hollowcube.replay.io.SegmentedFileReplaySource;
import dev.hollowcube.replay.io.SegmentedFileReplayStorage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.network.NetworkBuffer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The seam a later frame-mining job hangs off: whatever is attached to a compaction has to see the
/// same thing when it is run again over the object that compaction produced, or re-indexing after a
/// feature-set change would mean re-reading sources the sweeper has already deleted.
final class ReplayVisitorTest {

    @Test
    void aVisitorSeesTheSameTicksDrivenByACompactionAndByAWalkOverItsResult(@TempDir Path temporaryDirectory) {
        var storage = new SegmentedFileReplayStorage(temporaryDirectory);
        var recorder = ReplayRecorder.create(ReplayEvents.builder().build(), storage.writer("run", null),
            UUID.randomUUID(), ReplayHeader.worldVersion(UUID.randomUUID()), () -> {
            });
        for (var tick = 0; tick < 500; tick++) {
            recorder.submit(new AbsoluteMoveEvent(0, new Pos(tick, 64, 0), Vec.ZERO));
            recorder.advance();
        }
        recorder.finish(RunOutcome.COMPLETED).join();

        var recording = storage.load("run");
        assertNotNull(recording);

        var duringCompaction = new Collector();
        var compacted = ReplayCompactor.compact(recording.requirePreamble(),
            new SegmentedFileReplaySource(temporaryDirectory.resolve("run")), duringCompaction,
            twoChunks(recording.requirePreamble().index()));

        var overTheResult = new Collector();
        try (var reader = new CompactedReplayReader(compacted.data())) {
            ReplayVisitor.walk(reader, overTheResult);
        }

        // The boundaries differ — one side is the recorded chunks and the other the merged frames —
        // and everything a reader of the ticks can see does not.
        assertTrue(duringCompaction.boundaries.size() > overTheResult.boundaries.size(),
            "merging should have produced fewer frames than there were chunks");
        assertArrayEquals(duringCompaction.ticks.toByteArray(), overTheResult.ticks.toByteArray());
        assertEquals(500, duringCompaction.tickCount());
        assertEquals(500, overTheResult.tickCount());
        assertEquals(0, overTheResult.boundaries.getFirst().startTick());
    }

    /// A frame's snapshot flag is its first chunk's, and its ticks are the sum of the ones it
    /// merged. Everything else about a merged frame is new.
    @Test
    void mergingSumsTheTicksAndKeepsTheFirstChunksSnapshot(@TempDir Path temporaryDirectory) {
        var storage = new SegmentedFileReplayStorage(temporaryDirectory);
        var recorder = ReplayRecorder.create(ReplayEvents.builder().build(), storage.writer("run", null),
            UUID.randomUUID(), ReplayHeader.worldVersion(UUID.randomUUID()), () -> {
            });
        for (var tick = 0; tick < 500; tick++) {
            recorder.submit(new AbsoluteMoveEvent(0, new Pos(tick, 64, 0), Vec.ZERO));
            recorder.advance();
        }
        recorder.finish(RunOutcome.COMPLETED).join();

        var recording = storage.load("run");
        assertNotNull(recording);
        var chunks = recording.requirePreamble().index();
        var frameBytes = twoChunks(chunks);

        var compacted = ReplayCompactor.compact(recording.requirePreamble(),
            new SegmentedFileReplaySource(temporaryDirectory.resolve("run")), null, frameBytes);

        try (var reader = new CompactedReplayReader(compacted.data())) {
            var frames = reader.index();
            assertTrue(frames.size() < chunks.size(), frames.size() + " frames from " + chunks.size() + " chunks");
            assertEquals(frames.size(), reader.header().chunkCount());

            var tick = 0;
            var chunk = 0;
            for (var frame : frames) {
                assertEquals(tick, frame.startTick());
                assertEquals(chunks.get(chunk).flags(), frame.flags());
                assertTrue(frame.uncompressedLength() <= frameBytes);

                var merged = 0;
                while (merged < frame.tickCount()) merged += chunks.get(chunk++).tickCount();
                assertEquals(frame.tickCount(), merged, "a frame is a whole number of chunks");
                tick += frame.tickCount();
            }
            assertEquals(chunks.size(), chunk);
            assertEquals(reader.header().tickCount(), tick);
        }
    }

    @Test
    void oversizedChunksSplitAtTicksAndKeepOnlyTheirOriginalSnapshot(@TempDir Path directory) {
        var storage = new SegmentedFileReplayStorage(directory);
        var registry = ReplayEvents.builder()
            .register(HostEvent.class, NetworkBuffer.INT.transform(HostEvent::new, HostEvent::value)).build();
        var recorder = ReplayRecorder.create(registry, storage.writer("run", null),
            UUID.randomUUID(), new byte[0], () -> {});
        for (var tick = 0; tick < 200; tick++) {
            recorder.submit(new HostEvent(tick));
            for (var entity = 0; entity < 100; entity++)
                recorder.submit(new AbsoluteMoveEvent(entity, new Pos(tick, 64, entity), Vec.ZERO));
            recorder.advance();
        }
        recorder.finish(RunOutcome.COMPLETED).join();
        var preamble = storage.load("run").requirePreamble();
        assertEquals(1, preamble.index().size());
        assertTrue(preamble.index().getFirst().uncompressedLength() > ReplayCompactor.FRAME_BYTE_LIMIT);
        var original = new Collector();
        var compacted = ReplayCompactor.compact(preamble, new SegmentedFileReplaySource(directory.resolve("run")),
            original, registry);

        try (var reader = new CompactedReplayReader(compacted.data())) {
            assertTrue(reader.index().size() > 1);
            assertTrue(reader.index().getFirst().hasSnapshot());
            for (var frame : reader.index()) {
                assertTrue(frame.uncompressedLength() <= ReplayCompactor.FRAME_BYTE_LIMIT);
                if (frame.startTick() != 0) assertFalse(frame.hasSnapshot());
            }
            var rewritten = new Collector();
            ReplayVisitor.walk(reader, rewritten);
            assertArrayEquals(original.ticks.toByteArray(), rewritten.ticks.toByteArray());
            assertEquals(200, rewritten.tickCount());

            var played = new ArrayList<ReplayEvent>();
            var player = new ReplayPlayer(reader, registry, played::add);
            while (player.advance() == ReplayPlayer.Advance.ADVANCED) {}
            assertEquals(20_200, played.size());
            played.clear();
            var target = reader.index().getLast().startTick();
            player.seek(target);
            assertEquals(ReplayPlayer.Advance.ADVANCED, player.advance());
            assertEquals((target + 1) * 101, played.size());
            assertEquals(new AbsoluteMoveEvent(99, new Pos(target, 64, 99), Vec.ZERO), played.getLast());
        }
    }

    @Test
    void aTickLargerThanAFrameIsRefused(@TempDir Path directory) {
        var storage = new SegmentedFileReplayStorage(directory);
        var recorder = ReplayRecorder.create(ReplayEvents.builder().build(), storage.writer("run", null),
            UUID.randomUUID(), new byte[0], () -> {});
        for (var entity = 0; entity < 10_000; entity++)
            recorder.submit(new AbsoluteMoveEvent(entity, new Pos(0, 64, entity), Vec.ZERO));
        recorder.advance();
        recorder.finish(RunOutcome.COMPLETED).join();
        var preamble = storage.load("run").requirePreamble();
        var failure = assertThrows(IllegalStateException.class, () -> ReplayCompactor.compact(preamble,
            new SegmentedFileReplaySource(directory.resolve("run"))));
        assertTrue(failure.getMessage().contains("exceeds the frame limit"));
    }

    /// A limit that fits exactly two of the recorded chunks, so the framing is decided by the
    /// recording rather than by how many bytes an event happens to encode to.
    private static int twoChunks(List<ChunkIndex> chunks) {
        return 2 * chunks.getFirst().uncompressedLength();
    }

    private record HostEvent(int value) implements ReplayEvent {}

    private static final class Collector implements ReplayVisitor {
        private final ByteArrayOutputStream ticks = new ByteArrayOutputStream();
        private final List<ChunkIndex> boundaries = new ArrayList<>();

        @Override
        public void chunk(ChunkIndex source, NetworkBuffer decoded) {
            boundaries.add(source);
            ticks.writeBytes(decoded.read(NetworkBuffer.RAW_BYTES));
        }

        int tickCount() {
            return boundaries.stream().mapToInt(ChunkIndex::tickCount).sum();
        }
    }
}
