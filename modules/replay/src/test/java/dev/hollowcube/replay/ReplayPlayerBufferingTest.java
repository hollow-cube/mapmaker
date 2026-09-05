package dev.hollowcube.replay;

import dev.hollowcube.replay.data.ChunkIndex;
import dev.hollowcube.replay.data.ReplayHeader;
import dev.hollowcube.replay.event.AbsoluteMoveEvent;
import dev.hollowcube.replay.event.ReplayEvent;
import dev.hollowcube.replay.event.ReplayEvents;
import dev.hollowcube.replay.io.CompactedReplayReader;
import dev.hollowcube.replay.io.ReplayReader;
import dev.hollowcube.replay.io.RunOutcome;
import dev.hollowcube.replay.io.SegmentedFileReplaySource;
import dev.hollowcube.replay.io.SegmentedFileReplayStorage;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/// Covers playback over a reader that does not have every chunk yet, which is what a replay read
/// over the network in ranges looks like from here.
final class ReplayPlayerBufferingTest {
    /// Enough ticks that merging two chunks per frame still leaves several frames to stall on.
    private static final int TICKS = 1500;

    @Test
    void advanceStallsUntilTheChunkArrives(@TempDir Path temporaryDirectory) {
        var reader = new WithholdingReader(new CompactedReplayReader(record(temporaryDirectory)));
        var played = new ArrayList<ReplayEvent>();

        try (var player = new ReplayPlayer(reader, ReplayEvents.builder().build(), played::add)) {
            assertEquals(ReplayPlayer.Advance.STALLED, player.advance());
            assertEquals(0, player.tick());
            assertEquals(List.of(), played);
            assertEquals(List.of(0), reader.prefetched);

            reader.admit(0);
            assertEquals(ReplayPlayer.Advance.ADVANCED, player.advance());
            assertEquals(1, player.tick());
            assertEquals(List.of(move(0)), played);
        }
    }

    @Test
    void enteringAChunkPrefetchesTheNextOne(@TempDir Path temporaryDirectory) {
        var reader = new WithholdingReader(new CompactedReplayReader(record(temporaryDirectory)));
        var index = reader.index();
        assertTrue(index.size() >= 3, "the recording has to span several frames: " + index.size());
        reader.admit(index.get(0).startTick());
        reader.admit(index.get(1).startTick());

        try (var player = new ReplayPlayer(reader, ReplayEvents.builder().build(), event -> {
        })) {
            assertEquals(ReplayPlayer.Advance.ADVANCED, player.advance());
            assertEquals(List.of(index.get(1).startTick()), reader.prefetched);

            // Nothing more is asked for until the next chunk is actually entered.
            for (var tick = 1; tick < index.get(0).tickCount(); tick++)
                assertEquals(ReplayPlayer.Advance.ADVANCED, player.advance());
            assertEquals(List.of(index.get(1).startTick()), reader.prefetched);

            assertEquals(ReplayPlayer.Advance.ADVANCED, player.advance());
            assertEquals(List.of(index.get(1).startTick(), index.get(2).startTick()), reader.prefetched);
        }
    }

    @Test
    void seekingStallsUntilTheAnchorChunkArrives(@TempDir Path temporaryDirectory) {
        var reader = new WithholdingReader(new CompactedReplayReader(record(temporaryDirectory)));
        var played = new ArrayList<ReplayEvent>();
        var anchor = reader.index().get(1);
        // Inside the second frame, so seeking rewinds to its snapshot rather than to the start.
        var target = anchor.startTick() + anchor.tickCount() - 1;

        try (var player = new ReplayPlayer(reader, ReplayEvents.builder().build(), played::add)) {
            player.seek(target);
            assertEquals(target, player.tick());
            assertEquals(List.of(anchor.startTick()), reader.prefetched);

            assertEquals(ReplayPlayer.Advance.STALLED, player.advance());
            assertEquals(target, player.tick());
            assertEquals(List.of(), played);

            reader.admit(anchor.startTick());
            assertEquals(ReplayPlayer.Advance.ADVANCED, player.advance());
            assertEquals(target + 1, player.tick());
            // Rebuilding was deferred to advance(), so the anchor chunk replayed there first.
            assertEquals(move(anchor.startTick()), played.getFirst());
            assertEquals(move(target), played.getLast());
        }
    }

    private static byte[] record(Path temporaryDirectory) {
        var storage = new SegmentedFileReplayStorage(temporaryDirectory);
        var recorder = ReplayRecorder.create(
            ReplayEvents.builder().build(),
            storage.writer("run", null),
            UUID.randomUUID(),
            ReplayHeader.worldVersion(UUID.randomUUID()),
            () -> {
            }
        );
        for (var tick = 0; tick < TICKS; tick++) {
            recorder.submit(move(tick));
            recorder.advance();
        }
        recorder.finish(RunOutcome.COMPLETED).join();

        var recording = storage.load("run");
        assertNotNull(recording);
        // Two recorded chunks per compacted frame, so the boundaries the player buffers across are
        // merged ones rather than the ones the recorder wrote.
        return ReplayCompactor.compact(
            recording.requirePreamble(),
            new SegmentedFileReplaySource(temporaryDirectory.resolve("run")),
            null,
            2 * recording.requirePreamble().index().getFirst().uncompressedLength()
        ).data();
    }

    private static AbsoluteMoveEvent move(int tick) {
        return new AbsoluteMoveEvent(0, new Pos(tick, 64, 0), Vec.ZERO);
    }

    /// Hands over only the chunks a test has admitted, and records what was asked for.
    private static final class WithholdingReader implements ReplayReader {
        private final ReplayReader delegate;
        private final Set<Integer> resident = new HashSet<>();
        private final List<Integer> prefetched = new ArrayList<>();

        WithholdingReader(ReplayReader delegate) {
            this.delegate = delegate;
        }

        void admit(int startTick) {
            resident.add(startTick);
        }

        @Override
        public ReplayHeader header() {
            return delegate.header();
        }

        @Override
        public CompoundBinaryTag metadata() {
            return delegate.metadata();
        }

        @Override
        public List<ChunkIndex> index() {
            return delegate.index();
        }

        @Override
        public @Nullable NetworkBuffer chunk(ChunkIndex chunk) {
            return resident.contains(chunk.startTick()) ? delegate.chunk(chunk) : null;
        }

        @Override
        public void prefetch(ChunkIndex chunk) {
            prefetched.add(chunk.startTick());
        }

        @Override
        public void close() {
            delegate.close();
        }
    }
}
