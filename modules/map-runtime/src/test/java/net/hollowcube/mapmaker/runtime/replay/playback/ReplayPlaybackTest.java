package net.hollowcube.mapmaker.runtime.replay.playback;

import dev.hollowcube.replay.ReplayCompactor;
import dev.hollowcube.replay.ReplayPlayer;
import dev.hollowcube.replay.ReplayRecorder;
import dev.hollowcube.replay.data.ChunkIndex;
import dev.hollowcube.replay.data.ReplayHeader;
import dev.hollowcube.replay.event.AbsoluteMoveEvent;
import dev.hollowcube.replay.event.ReplayEvent;
import dev.hollowcube.replay.event.ReplayEvents;
import dev.hollowcube.replay.io.CompactedReplayReader;
import dev.hollowcube.replay.io.SegmentedFileReplaySource;
import dev.hollowcube.replay.io.ReplayReader;
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

/// Covers the speed budget and seeking, both of which are pure arithmetic over a replay and need
/// no world to be wrong in.
final class ReplayPlaybackTest {
    private static final int TICK_COUNT = 100;

    @Test
    void pausedPlaybackPlaysNothing(@TempDir Path temporaryDirectory) {
        var playback = playback(temporaryDirectory, new ArrayList<>());

        for (var i = 0; i < 10; i++) playback.tick();
        assertEquals(0, playback.currentTick());
    }

    @Test
    void normalSpeedPlaysOneTickPerServerTick(@TempDir Path temporaryDirectory) {
        var playback = playback(temporaryDirectory, new ArrayList<>());
        playback.play();

        for (var i = 0; i < 10; i++) playback.tick();
        assertEquals(10, playback.currentTick());
    }

    @Test
    void halfSpeedPlaysEveryOtherServerTick(@TempDir Path temporaryDirectory) {
        var playback = playback(temporaryDirectory, new ArrayList<>());
        playback.setSpeed(0.5);
        playback.play();

        playback.tick();
        assertEquals(0, playback.currentTick());
        playback.tick();
        assertEquals(1, playback.currentTick());
        playback.tick();
        assertEquals(1, playback.currentTick());
        playback.tick();
        assertEquals(2, playback.currentTick());
    }

    @Test
    void fractionalSpeedAboveOneKeepsItsRemainder(@TempDir Path temporaryDirectory) {
        var playback = playback(temporaryDirectory, new ArrayList<>());
        playback.setSpeed(1.5);
        playback.play();

        playback.tick();
        assertEquals(1, playback.currentTick());
        playback.tick();
        assertEquals(3, playback.currentTick());
        playback.tick();
        assertEquals(4, playback.currentTick());
        playback.tick();
        assertEquals(6, playback.currentTick());
    }

    @Test
    void doubleSpeedPlaysTwoTicksPerServerTick(@TempDir Path temporaryDirectory) {
        var events = new ArrayList<ReplayEvent>();
        var playback = playback(temporaryDirectory, events);
        playback.setSpeed(2);
        playback.play();

        playback.tick();
        assertEquals(2, playback.currentTick());
        assertEquals(List.of(move(0), move(1)), events);
    }

    @Test
    void reachingTheEndPausesAndStops(@TempDir Path temporaryDirectory) {
        var playback = playback(temporaryDirectory, new ArrayList<>());
        playback.setSpeed(8);
        playback.play();

        for (var i = 0; i < 20; i++) playback.tick();
        assertEquals(TICK_COUNT, playback.currentTick());
        assertTrue(playback.finished());
        assertFalse(playback.playing());
    }

    @Test
    void loopingWrapsWithinASingleServerTick(@TempDir Path temporaryDirectory) {
        var events = new ArrayList<ReplayEvent>();
        var playback = playback(temporaryDirectory, events);
        playback.setLooping(true);
        playback.setSpeed(2);
        playback.play();

        playback.seek(TICK_COUNT - 1);
        events.clear();

        playback.tick();
        assertEquals(1, playback.currentTick());
        assertTrue(playback.playing());
        assertEquals(List.of(move(TICK_COUNT - 1), move(0)), events);
    }

    @Test
    void seekingDropsTheAccumulatedBudget(@TempDir Path temporaryDirectory) {
        var events = new ArrayList<ReplayEvent>();
        var playback = playback(temporaryDirectory, events);
        playback.setSpeed(0.5);
        playback.play();

        // Half a tick of budget, which the seek must not carry into the new position.
        playback.tick();
        playback.seek(40);
        events.clear();

        playback.tick();
        assertEquals(40, playback.currentTick());
        playback.tick();
        assertEquals(41, playback.currentTick());
        assertEquals(List.of(move(40)), events);
    }

    @Test
    void stallingDropsTheBudgetItCannotSpend(@TempDir Path temporaryDirectory) {
        var events = new ArrayList<ReplayEvent>();
        var reader = new WithholdingReader(new CompactedReplayReader(record(temporaryDirectory)));
        var playback = new ReplayPlayback(
            new ReplayPlayer(reader, ReplayEvents.builder().build(), events::add));
        playback.play();

        for (var i = 0; i < 5; i++) playback.tick();
        assertEquals(0, playback.currentTick());
        assertTrue(playback.buffering());
        assertEquals(List.of(), events);

        // The five buffered server ticks are gone rather than owed, so nothing fast forwards.
        reader.admit(0);
        playback.tick();
        assertEquals(1, playback.currentTick());
        assertFalse(playback.buffering());
        assertEquals(List.of(move(0)), events);
    }

    private static ReplayPlayback playback(Path temporaryDirectory, List<ReplayEvent> events) {
        return new ReplayPlayback(new ReplayPlayer(
            new CompactedReplayReader(record(temporaryDirectory)),
            ReplayEvents.builder().build(),
            events::add
        ));
    }

    private static byte[] record(Path temporaryDirectory) {
        var registry = ReplayEvents.builder().build();
        var storage = new SegmentedFileReplayStorage(temporaryDirectory);

        var recorder = ReplayRecorder.create(
            registry,
            storage.writer("run", null),
            UUID.randomUUID(),
            ReplayHeader.worldVersion(UUID.randomUUID()),
            () -> {
            }
        );
        for (var tick = 0; tick < TICK_COUNT; tick++) {
            recorder.submit(move(tick));
            recorder.advance();
        }
        recorder.finish().join();

        var recording = storage.load("run");
        assertNotNull(recording);
        var compacted = ReplayCompactor.compact(
            recording.requirePreamble(),
            new SegmentedFileReplaySource(temporaryDirectory.resolve("run"))
        );

        return compacted.data();
    }

    private static AbsoluteMoveEvent move(int tick) {
        return new AbsoluteMoveEvent(ReplayScene.SUBJECT_ENTITY_ID, new Pos(tick, 64, 0), Vec.ZERO);
    }

    /// Hands over only the chunks a test has admitted, so playback can be stalled on purpose.
    private static final class WithholdingReader implements ReplayReader {
        private final ReplayReader delegate;
        private final Set<Integer> resident = new HashSet<>();

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
            // Tests admit chunks by hand.
        }

        @Override
        public void close() {
            delegate.close();
        }
    }
}
