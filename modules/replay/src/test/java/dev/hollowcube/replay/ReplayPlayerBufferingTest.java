package dev.hollowcube.replay;

import dev.hollowcube.replay.data.ChunkIndex;
import dev.hollowcube.replay.data.ReplayHeader;
import dev.hollowcube.replay.event.AbsoluteMoveEvent;
import dev.hollowcube.replay.event.ReplayEvent;
import dev.hollowcube.replay.event.ReplayEvents;
import dev.hollowcube.replay.io.CompactedReplayReader;
import dev.hollowcube.replay.io.ReplayReader;
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
    // Matches the recorder's chunk tick limit, so chunk N starts at tick N * 200.
    private static final int CHUNK_TICKS = 200;

    @Test
    void advanceStallsUntilTheChunkArrives(@TempDir Path temporaryDirectory) {
        var reader = new WithholdingReader(new CompactedReplayReader(record(temporaryDirectory, 500)));
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
        var reader = new WithholdingReader(new CompactedReplayReader(record(temporaryDirectory, 500)));
        reader.admit(0);
        reader.admit(CHUNK_TICKS);

        try (var player = new ReplayPlayer(reader, ReplayEvents.builder().build(), event -> {
        })) {
            assertEquals(ReplayPlayer.Advance.ADVANCED, player.advance());
            assertEquals(List.of(CHUNK_TICKS), reader.prefetched);

            // Nothing more is asked for until the next chunk is actually entered.
            for (var tick = 1; tick < CHUNK_TICKS; tick++)
                assertEquals(ReplayPlayer.Advance.ADVANCED, player.advance());
            assertEquals(List.of(CHUNK_TICKS), reader.prefetched);

            assertEquals(ReplayPlayer.Advance.ADVANCED, player.advance());
            assertEquals(List.of(CHUNK_TICKS, 2 * CHUNK_TICKS), reader.prefetched);
        }
    }

    @Test
    void seekingStallsUntilTheAnchorChunkArrives(@TempDir Path temporaryDirectory) {
        var reader = new WithholdingReader(new CompactedReplayReader(record(temporaryDirectory, 500)));
        var played = new ArrayList<ReplayEvent>();

        try (var player = new ReplayPlayer(reader, ReplayEvents.builder().build(), played::add)) {
            player.seek(380);
            assertEquals(380, player.tick());
            assertEquals(List.of(CHUNK_TICKS), reader.prefetched);

            assertEquals(ReplayPlayer.Advance.STALLED, player.advance());
            assertEquals(380, player.tick());
            assertEquals(List.of(), played);

            reader.admit(CHUNK_TICKS);
            assertEquals(ReplayPlayer.Advance.ADVANCED, player.advance());
            assertEquals(381, player.tick());
            // Rebuilding was deferred to advance(), so the anchor chunk replayed there first.
            assertEquals(move(200), played.getFirst());
            assertEquals(move(380), played.getLast());
        }
    }

    private static byte[] record(Path temporaryDirectory, int tickCount) {
        var storage = new SegmentedFileReplayStorage(temporaryDirectory);
        var recorder = ReplayRecorder.create(
            ReplayEvents.builder().build(),
            storage.writer("run", null),
            UUID.randomUUID(),
            ReplayHeader.worldVersion(UUID.randomUUID()),
            () -> {
            }
        );
        for (var tick = 0; tick < tickCount; tick++) {
            recorder.submit(move(tick));
            recorder.advance();
        }
        recorder.finish().join();

        var recording = storage.load("run");
        assertNotNull(recording);
        return ReplayCompactor.compact(
            recording.requirePreamble(),
            new SegmentedFileReplaySource(temporaryDirectory.resolve("run"))
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
