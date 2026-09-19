package net.hollowcube.apiworker.jobs;

import dev.hollowcube.replay.ReplayCompactor;
import dev.hollowcube.replay.data.ChunkIndex;
import dev.hollowcube.replay.data.ReplayHeader;
import dev.hollowcube.replay.data.ReplayPreamble;
import dev.hollowcube.replay.io.CompactedReplayReader;
import net.hollowcube.apiserver.job.CompactReplay;
import net.hollowcube.apiserver.job.TranscodeReplay;
import net.hollowcube.ipc.Blob;
import net.hollowcube.ipc.replay.ReplayCommit;
import net.hollowcube.ipc.replay.ReplayCompaction;
import net.hollowcube.ipc.replay.ReplayInfo;
import net.hollowcube.ipc.replay.ReplayOutcome;
import net.hollowcube.ipc.replay.ReplayRepresentation;
import net.hollowcube.ipc.replay.ReplayService;
import net.hollowcube.ipc.replay.ReplayState;
import net.hollowcube.ipc.util.IpcException;
import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranscodeReplayRunnerTest {

    static final String ID = "save-state-1";
    static final Duration IDLE = Duration.ofDays(7);

    @Test
    void run_rewritesACompactedFormat4ReplayAsOneThatPlaysBackTheSame() throws Exception {
        var original = LegacyReplays.compacted();
        var storage = FakeStorage.compacted(original);

        new TranscodeReplayRunner(storage, IDLE).run(new TranscodeReplay(ID, "backfill"));

        var published = storage.published.getFirst();
        assertEquals("transcode:" + ID + ":3", published.meta().idempotencyKey());
        assertEquals(3, published.meta().expectedRevision());
        assertCurrent(published.body());
        assertEquals(LegacyReplays.play(original), LegacyReplays.play(published.body()));
        assertEquals(LegacyReplays.EVENTS, LegacyReplays.play(published.body()).subList(0, 2));
        assertEquals(LegacyReplays.play(original), LegacyReplays.events(published.body()));
    }

    @Test
    void run_rewritesASegmentedFormat4ReplayAsOneThatPlaysBackTheSame() throws Exception {
        var recording = LegacyReplays.recording();
        var storage = FakeStorage.segmented(recording);

        new TranscodeReplayRunner(storage, IDLE).run(new TranscodeReplay(ID, "backfill"));

        var published = storage.published.getFirst();
        assertCurrent(published.body());
        var untranscoded = FakeStorage.segmented(recording);
        var stored = StoredReplay.read(
            untranscoded,
            untranscoded.getReplay(ID),
            recording.preamble()
        );
        var legacy = stored.untranscoded();
        assertEquals(LegacyReplays.play(legacy), LegacyReplays.play(published.body()));
        assertEquals(LegacyReplays.EVENTS, LegacyReplays.play(published.body()).subList(0, 2));
        assertEquals(LegacyReplays.play(legacy), LegacyReplays.events(published.body()));
    }

    @Test
    void compaction_transcodesAFormat4Recording() throws Exception {
        var storage = FakeStorage.segmented(LegacyReplays.recording());

        new CompactReplayRunner(storage).run(new CompactReplay(ID, "reconcile"));

        assertCurrent(storage.published.getFirst().body());
    }

    @Test
    void run_leavesAReplayRecordedIntoRecentlyAlreadyCurrentOrGoneAlone() throws Exception {
        var recording = FakeStorage.segmented(LegacyReplays.recording());
        recording.state = ReplayState.RECORDING;
        recording.updatedAt = System.currentTimeMillis();
        new TranscodeReplayRunner(recording, IDLE).run(new TranscodeReplay(ID, "backfill"));
        assertEquals(List.of(), recording.published);
        assertEquals(List.of(), recording.committed);

        var legacy = FakeStorage.compacted(LegacyReplays.compacted());
        new TranscodeReplayRunner(legacy, IDLE).run(new TranscodeReplay(ID, "backfill"));
        var current = FakeStorage.compacted(legacy.published.getFirst().body());
        new TranscodeReplayRunner(current, IDLE).run(new TranscodeReplay(ID, "backfill"));
        assertEquals(List.of(), current.published);

        new TranscodeReplayRunner(current, IDLE)
            .run(new TranscodeReplay("never-recorded", "manual"));
        assertEquals(List.of(), current.published);
    }

    @Test
    void run_rewritesAnIdleFormat4RecordingInPlace() throws Exception {
        var recording = LegacyReplays.recording();
        var storage = FakeStorage.segmented(recording);
        storage.state = ReplayState.RECORDING;

        new TranscodeReplayRunner(storage, IDLE).run(new TranscodeReplay(ID, "backfill"));

        assertEquals(List.of(), storage.published);
        var commit = storage.committed.getFirst();
        var segmentIndex = recording.segments().size();
        assertEquals(3, commit.meta().expectedRevision());
        assertEquals("transcode:" + ID + ":3", commit.meta().idempotencyKey());
        assertEquals(segmentIndex, commit.meta().segmentIndex());
        assertFalse(commit.meta().finished());

        var preamble = ReplayPreamble.read(
            Arrays.copyOf(commit.body(), commit.meta().preambleLength())
        );
        var segment = Arrays.copyOfRange(
            commit.body(),
            commit.meta().preambleLength(),
            commit.body().length
        );
        for (var chunk : preamble.index()) {
            assertEquals(ReplayHeader.VERSION_LATEST, chunk.formatVersion(), chunk.toString());
            assertEquals(MinecraftServer.DATA_VERSION, chunk.dataVersion(), chunk.toString());
            assertEquals(segmentIndex, ReplayPreamble.segmentIndex(chunk));
        }
        assertEquals(segmentIndex + 1, preamble.nextSegmentIndex());

        var rewritten = ReplayCompactor.compact(preamble, index -> {
            assertEquals(segmentIndex, index);
            return segment;
        });
        var legacy = StoredReplay
            .read(FakeStorage.segmented(recording), storage.getReplay(ID), recording.preamble())
            .untranscoded();
        assertEquals(LegacyReplays.play(legacy), LegacyReplays.play(rewritten.data()));
        assertEquals(LegacyReplays.EVENTS, LegacyReplays.play(rewritten.data()).subList(0, 2));
    }

    @Test
    void run_returnsQuietlyWhenARecordingMovedOnUnderTheRewrite() throws Exception {
        for (var status : List.of(412, 409)) {
            var storage = FakeStorage.segmented(LegacyReplays.recording());
            storage.state = ReplayState.RECORDING;
            storage.commitStatus = status;

            new TranscodeReplayRunner(storage, IDLE).run(new TranscodeReplay(ID, "backfill"));

            assertEquals(1, storage.committed.size());
        }
    }

    @Test
    void run_returnsQuietlyWhenItLosesThePublication() throws Exception {
        var storage = FakeStorage.compacted(LegacyReplays.compacted());
        storage.publishStatus = 412;

        new TranscodeReplayRunner(storage, IDLE).run(new TranscodeReplay(ID, "backfill"));

        assertEquals(1, storage.published.size());
    }

    @Test
    void run_returnsQuietlyWhenTheReplayMovedOnBeforeItsPreambleWasRead() throws Exception {
        var storage = FakeStorage.compacted(LegacyReplays.compacted());
        storage.preambleStatus = 412;

        new TranscodeReplayRunner(storage, IDLE).run(new TranscodeReplay(ID, "backfill"));

        assertEquals(List.of(), storage.published);
    }

    @Test
    void run_letsAnythingElseTheStoreSaysFailTheRow() {
        var storage = FakeStorage.compacted(LegacyReplays.compacted());
        storage.publishStatus = 500;

        assertThrows(
            IpcException.class,
            () -> new TranscodeReplayRunner(storage, IDLE).run(new TranscodeReplay(ID, "backfill"))
        );
    }

    private static void assertCurrent(byte[] compacted) {
        assertEquals(ReplayHeader.VERSION_LATEST, ReplayHeader.versionOf(compacted));
        for (var chunk : ReplayPreamble.index(compacted)) {
            assertEquals(ReplayHeader.VERSION_LATEST, chunk.formatVersion(), chunk.toString());
            assertEquals(MinecraftServer.DATA_VERSION, chunk.dataVersion(), chunk.toString());
        }
        assertEquals(
            LegacyReplays.TICKS.size(),
            ReplayPreamble.index(compacted).stream().mapToInt(ChunkIndex::tickCount).sum()
        );
    }

    static final class FakeStorage implements ReplayService {
        final List<Published> published = new ArrayList<>();
        final List<Committed> committed = new ArrayList<>();

        private final byte[] preamble;
        private final List<byte[]> segments;
        private final byte @Nullable [] compacted;
        ReplayState state = ReplayState.FINISHED;
        long updatedAt = 0;
        int publishStatus = 200;
        int commitStatus = 200;
        int preambleStatus = 200;

        record Published(ReplayCompaction meta, byte[] body) {}

        record Committed(ReplayCommit meta, byte[] body) {}

        private FakeStorage(byte[] preamble, List<byte[]> segments, byte @Nullable [] compacted) {
            this.preamble = preamble;
            this.segments = segments;
            this.compacted = compacted;
        }

        static FakeStorage segmented(LegacyReplays.Recording recording) {
            return new FakeStorage(recording.preamble(), recording.segments(), null);
        }

        static FakeStorage compacted(byte[] compacted) {
            var preambleLength = ReplayHeader.HEADER_LENGTH;
            try (var reader = new CompactedReplayReader(compacted)) {
                preambleLength += reader.header().metadataLength() + reader.header().indexLength();
            }
            return new FakeStorage(Arrays.copyOf(compacted, preambleLength), List.of(), compacted);
        }

        @Override
        public @Nullable ReplayInfo getReplay(String id) {
            if (!ID.equals(id)) return null;
            return new ReplayInfo(
                id,
                3,
                state,
                compacted == null ? ReplayRepresentation.SEGMENTED : ReplayRepresentation.COMPACTED,
                compacted == null ? segments.size() : null,
                preamble.length,
                ReplayOutcome.RESET,
                updatedAt
            );
        }

        @Override
        public Blob getPreamble(String id, @Nullable Long expectedRevision) {
            if (preambleStatus != 200) throw new IpcException(preambleStatus, "moved on");
            return Blob.of(preamble);
        }

        @Override
        public Blob getSegment(String id, int segmentIndex) {
            if (segmentIndex >= segments.size())
                throw new IpcException(404, "no segment " + segmentIndex);
            return Blob.of(segments.get(segmentIndex));
        }

        @Override
        public Blob getCompacted(String id, @Nullable Long start, @Nullable Long endInclusive) {
            if (compacted == null) throw new IpcException(409, "replay_not_compacted");
            return Blob.of(compacted);
        }

        @Override
        public ReplayInfo commit(ReplayCommit meta, Blob body) {
            if (state == ReplayState.FINISHED)
                throw new AssertionError("a finished replay is published, not committed");
            try {
                committed.add(new Committed(meta, body.readAllBytes()));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            if (commitStatus != 200)
                throw new IpcException(commitStatus, "answered " + commitStatus);
            return getReplay(meta.id());
        }

        @Override
        public int dropSegments(String id) {
            throw new AssertionError("the sweeper does that, not a rewrite");
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
            return getReplay(meta.id());
        }
    }
}
