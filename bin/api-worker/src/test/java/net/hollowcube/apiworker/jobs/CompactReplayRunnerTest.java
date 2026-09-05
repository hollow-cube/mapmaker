package net.hollowcube.apiworker.jobs;

import dev.hollowcube.replay.ReplayPlayer;
import dev.hollowcube.replay.ReplayRecorder;
import dev.hollowcube.replay.data.ReplayHeader;
import dev.hollowcube.replay.event.AbsoluteMoveEvent;
import dev.hollowcube.replay.event.ReplayEvent;
import dev.hollowcube.replay.event.ReplayEvents;
import dev.hollowcube.replay.io.CompactedReplayReader;
import dev.hollowcube.replay.io.RunOutcome;
import dev.hollowcube.replay.io.SegmentedFileReplayStorage;
import net.hollowcube.apiserver.job.CompactReplay;
import net.hollowcube.ipc.Blob;
import net.hollowcube.ipc.replay.ReplayCommit;
import net.hollowcube.ipc.replay.ReplayCompaction;
import net.hollowcube.ipc.replay.ReplayInfo;
import net.hollowcube.ipc.replay.ReplayOutcome;
import net.hollowcube.ipc.replay.ReplayRepresentation;
import net.hollowcube.ipc.replay.ReplayService;
import net.hollowcube.ipc.replay.ReplayState;
import net.hollowcube.ipc.util.IpcException;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The compaction job against a stand-in for storage.
///
/// What storage itself does with a publication is [net.hollowcube.apiserver.replay] territory and is
/// tested there; what matters here is that the runner reads a real recording, produces something
/// that plays back tick for tick, and derives the same idempotency key every time it is run against
/// the same source revision.
class CompactReplayRunnerTest {

    private static final String ID = "save-state-1";
    private static final int TICKS = 500;

    @Test
    void run_compactsAFinishedRecordingIntoSomethingThatPlaysBackTickForTick(@TempDir Path directory) throws Exception {
        var storage = new FakeStorage();
        var expected = record(directory, storage);

        new CompactReplayRunner(storage).run(new CompactReplay(ID, "final-commit"));

        assertEquals(1, storage.published.size());
        var published = storage.published.getFirst();
        assertEquals("compact:" + ID + ":" + (storage.version - 1), published.meta().idempotencyKey());

        var played = new ArrayList<ReplayEvent>();
        try (var player = new ReplayPlayer(new CompactedReplayReader(published.body()),
            ReplayEvents.builder().build(), played::add)) {
            assertEquals(TICKS, player.tickCount());
            while (player.advance() == ReplayPlayer.Advance.ADVANCED) ;
        }
        assertEquals(expected, played);
    }

    @Test
    void run_isANoOpForAReplayThatIsGoneStillRecordingOrAlreadyCompacted(@TempDir Path directory) throws Exception {
        var storage = new FakeStorage();

        new CompactReplayRunner(storage).run(new CompactReplay("never-recorded", "reconcile"));
        assertEquals(List.of(), storage.published);

        record(directory, storage);
        storage.state = ReplayState.RECORDING;
        new CompactReplayRunner(storage).run(new CompactReplay(ID, "reconcile"));
        assertEquals(List.of(), storage.published);

        storage.state = ReplayState.FINISHED;
        storage.representation = ReplayRepresentation.COMPACTED;
        new CompactReplayRunner(storage).run(new CompactReplay(ID, "reconcile"));
        assertEquals(List.of(), storage.published);
    }

    /// A commit that landed while the runner was compacting. There is a fresh row for it already.
    @Test
    void run_returnsQuietlyWhenThePublicationLosesItsPrecondition(@TempDir Path directory) throws Exception {
        var storage = new FakeStorage();
        record(directory, storage);
        storage.publishStatus = 412;

        new CompactReplayRunner(storage).run(new CompactReplay(ID, "final-commit"));
    }

    @Test
    void run_letsAnythingElseTheStoreSaysParkTheRow(@TempDir Path directory) throws Exception {
        var storage = new FakeStorage();
        record(directory, storage);
        storage.publishStatus = 500;

        assertThrows(IpcException.class,
            () -> new CompactReplayRunner(storage).run(new CompactReplay(ID, "final-commit")));
    }

    /// The fix for the orphaning bug: two runs against one source revision derive one key, so the
    /// second is answered from the idempotency record rather than uploading a second object.
    @Test
    void run_derivesTheSameKeyFromTheSameSourceRevision(@TempDir Path directory) throws Exception {
        var storage = new FakeStorage();
        record(directory, storage);
        // The publication that was made but whose answer was lost, so the row is picked again.
        storage.applyPublications = false;

        new CompactReplayRunner(storage).run(new CompactReplay(ID, "final-commit"));
        new CompactReplayRunner(storage).run(new CompactReplay(ID, "final-commit"));

        assertEquals(2, storage.published.size());
        assertEquals(storage.published.get(0).meta().idempotencyKey(),
            storage.published.get(1).meta().idempotencyKey());
        assertTrue(storage.published.getFirst().meta().idempotencyKey().startsWith("compact:" + ID + ":"));
    }

    @Test
    void compactionKey_hashesOneThatWouldNotFitTheColumn() {
        var oversizedId = "r".repeat(512);

        var key = CompactReplayRunner.compactionKey(oversizedId, 3);

        assertTrue(key.length() <= 512, "key is " + key.length() + " characters");
        assertEquals(key, CompactReplayRunner.compactionKey(oversizedId, 3));
        // Still a different key for a different source revision.
        assertNotEquals(key, CompactReplayRunner.compactionKey(oversizedId, 4));
    }

    /// Records a real replay into `storage`, and answers the events it should play back as.
    private static List<ReplayEvent> record(Path directory, FakeStorage storage) {
        var files = new SegmentedFileReplayStorage(directory);
        var recorder = ReplayRecorder.create(ReplayEvents.builder().build(), files.writer(ID, null),
            UUID.randomUUID(), ReplayHeader.worldVersion(UUID.randomUUID()), () -> {
            });

        var expected = new ArrayList<ReplayEvent>();
        for (var tick = 0; tick < TICKS; tick++) {
            var event = new AbsoluteMoveEvent(0, new Pos(tick, 64, 0), Vec.ZERO);
            recorder.submit(event);
            expected.add(event);
            recorder.advance();
        }
        recorder.finish(RunOutcome.COMPLETED).join();

        var recording = files.load(ID);
        assertNotNull(recording);
        storage.load(directory.resolve(ID), recording.requirePreamble().nextSegmentIndex());
        return expected;
    }

    /// Storage as far as the runner can tell: a preamble, some segments, and whatever it is told
    /// about the state of the replay.
    private static final class FakeStorage implements ReplayService {
        private final Map<Integer, byte[]> segments = new LinkedHashMap<>();
        private final List<Published> published = new ArrayList<>();

        private byte @Nullable [] preamble;
        private ReplayState state = ReplayState.FINISHED;
        private ReplayRepresentation representation = ReplayRepresentation.SEGMENTED;
        private long version = 3;
        private int publishStatus = 200;
        private boolean applyPublications = true;

        private record Published(ReplayCompaction meta, byte[] body) {
        }

        /// The names `SegmentedFileReplayWriter` lays a local recording out under; they are
        /// package-private to it, so this is the one place they are spelled out again.
        void load(Path directory, int segmentCount) {
            try {
                preamble = Files.readAllBytes(directory.resolve("preamble.dat"));
                for (var index = 0; index < segmentCount; index++)
                    segments.put(index, Files.readAllBytes(
                        directory.resolve(String.format("segment-%03d.dat", index))));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public @Nullable ReplayInfo getReplay(String id) {
            if (!ID.equals(id) || preamble == null) return null;
            return new ReplayInfo(id, version, state, representation,
                representation == ReplayRepresentation.SEGMENTED ? segments.size() : null,
                preamble.length, ReplayOutcome.RESET, 0);
        }

        @Override
        public Blob getPreamble(String id, @Nullable Long expectedRevision) {
            if (preamble == null) throw new IpcException(404, "no replay " + id);
            if (expectedRevision != null && expectedRevision != version)
                throw new IpcException(412, "moved on");
            return Blob.of(preamble);
        }

        @Override
        public Blob getSegment(String id, int segmentIndex) {
            var segment = segments.get(segmentIndex);
            if (segment == null) throw new IpcException(404, "no segment " + segmentIndex);
            return Blob.of(segment);
        }

        @Override
        public Blob getCompacted(String id, @Nullable Long start, @Nullable Long endInclusive) {
            throw new IpcException(409, "replay_not_compacted");
        }

        @Override
        public ReplayInfo commit(ReplayCommit meta, Blob body) {
            throw new UnsupportedOperationException("the runner never commits");
        }

        @Override
        public int dropSegments(String id) {
            throw new UnsupportedOperationException("the sweeper does that, not this runner");
        }

        @Override
        public ReplayInfo publishCompacted(ReplayCompaction meta, Blob body) {
            try {
                published.add(new Published(meta, body.readAllBytes()));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            if (publishStatus != 200)
                throw new IpcException(publishStatus, "answered " + publishStatus);
            if (applyPublications) {
                representation = ReplayRepresentation.COMPACTED;
                version++;
            }
            return getReplay(meta.id());
        }
    }
}
