package net.hollowcube.mapmaker.runtime.replay;

import dev.hollowcube.replay.data.ReplayHeader;
import dev.hollowcube.replay.io.RunOutcome;
import dev.hollowcube.replay.io.SegmentedReplayCommit;
import dev.hollowcube.replay.io.SegmentedReplayWriter;
import net.hollowcube.ipc.Blob;
import net.hollowcube.ipc.replay.ReplayCommit;
import net.hollowcube.ipc.replay.ReplayCompaction;
import net.hollowcube.ipc.replay.ReplayInfo;
import net.hollowcube.ipc.replay.ReplayOutcome;
import net.hollowcube.ipc.replay.ReplayRepresentation;
import net.hollowcube.ipc.replay.ReplayService;
import net.hollowcube.ipc.replay.ReplayState;
import net.hollowcube.ipc.util.IpcException;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The recorder's side of the replay ipc contract: what a commit says about itself, and what a
/// resume reads back.
final class ApiSegmentedReplayStorageTest {

    private static final String ID = "save-state-1";
    private static final byte[] PREAMBLE = "PREAMBLE".getBytes(StandardCharsets.UTF_8);
    private static final byte[] SEGMENT = "SEGMENT".getBytes(StandardCharsets.UTF_8);

    private final FakeService service = new FakeService();
    private final ApiSegmentedReplayStorage storage = new ApiSegmentedReplayStorage(service);

    @Test
    void load_isNullForOneNothingHasCommitted() {
        assertNull(storage.load(ID));
    }

    @Test
    void load_readsThePreambleAtTheRevisionTheStateCameFrom() {
        service.info = info(ReplayState.RECORDING, ReplayRepresentation.SEGMENTED, 0);

        var loaded = storage.load(ID);

        assertNotNull(loaded);
        assertEquals("4", loaded.token());
        assertFalse(loaded.finished());
        assertEquals(4L, service.preambleRevision);
    }

    /// A compacted preamble carries absolute offsets, so there is nothing here to resume from — but
    /// the recorder still has to be able to tell it apart from a replay that was never recorded.
    @Test
    void load_ofACompactedReplayIsFinishedWithNoPreamble() {
        service.info = info(ReplayState.FINISHED, ReplayRepresentation.COMPACTED, null);

        var loaded = storage.load(ID);

        assertNotNull(loaded);
        assertNull(loaded.preamble());
        assertTrue(loaded.finished());
        assertThrows(IllegalStateException.class, loaded::requirePreamble);
    }

    @Test
    void load_refusesAPreambleThatDisagreesWithTheNextSegmentIndex() {
        service.info = info(ReplayState.RECORDING, ReplayRepresentation.SEGMENTED, 7);

        assertThrows(IllegalStateException.class, () -> storage.load(ID));
    }

    @Test
    void commit_sendsTheBodyItsLengthAndItsDigest() {
        var writer = storage.writer(ID, null);

        writer.commit(new SegmentedReplayCommit(UUID.randomUUID(), PREAMBLE, 0, SEGMENT, false, null));

        var sent = service.commits.getFirst();
        assertNull(sent.meta().expectedRevision());
        assertEquals(PREAMBLE.length, sent.meta().preambleLength());
        assertEquals(0, sent.meta().segmentIndex());
        assertFalse(sent.meta().finished());
        assertNull(sent.meta().outcome());
        assertArrayEquals(concat(PREAMBLE, SEGMENT), sent.body());
        assertEquals(digest(concat(PREAMBLE, SEGMENT)), sent.meta().contentDigest());
    }

    /// The one thing the api cannot work out for itself. A completed run and the hard reset that
    /// superseded one are the same shape of commit and ~95% of them are resets.
    @Test
    void commit_carriesWhyTheRecordingIsBeingFinished() {
        var writer = storage.writer(ID, null);

        writer.commit(new SegmentedReplayCommit(UUID.randomUUID(), PREAMBLE, 0, SEGMENT, true, RunOutcome.COMPLETED));
        writer.commit(new SegmentedReplayCommit(UUID.randomUUID(), PREAMBLE, 1, SEGMENT, true, RunOutcome.RESET));

        assertEquals(ReplayOutcome.FINISHED, service.commits.get(0).meta().outcome());
        assertEquals(ReplayOutcome.RESET, service.commits.get(1).meta().outcome());
    }

    @Test
    void commit_carriesTheRevisionTheLastOneAnswered() {
        var writer = resumingWriter();

        writer.commit(new SegmentedReplayCommit(UUID.randomUUID(), PREAMBLE, 0, SEGMENT, false, null));
        writer.commit(new SegmentedReplayCommit(UUID.randomUUID(), PREAMBLE, 1, SEGMENT, false, null));

        assertEquals(4L, service.commits.get(0).meta().expectedRevision());
        assertEquals(5L, service.commits.get(1).meta().expectedRevision());
    }

    /// Commits cannot be merged, so a recorder that has been overtaken stops rather than retrying
    /// into a divergence.
    @Test
    void commit_stopsRatherThanRetryingWhenSomethingElseAdvancedTheRecording() {
        service.status = 409;
        var writer = resumingWriter();

        assertThrows(IllegalStateException.class, () ->
            writer.commit(new SegmentedReplayCommit(UUID.randomUUID(), PREAMBLE, 0, SEGMENT, false, null)));
        assertEquals(1, service.commits.size());
    }

    @Test
    void commit_retriesTheIdenticalRequestWhenTheStoreIsUnwell() {
        service.status = 500;
        var writer = resumingWriter();

        assertThrows(IpcException.class, () ->
            writer.commit(new SegmentedReplayCommit(UUID.randomUUID(), PREAMBLE, 0, SEGMENT, false, null)));

        assertEquals(3, service.commits.size());
        // Byte for byte the same request every time, which is what makes the retry safe.
        assertEquals(service.commits.get(0).meta(), service.commits.get(2).meta());
    }

    /// A writer bound to a recording that already exists, so its first commit is conditional on
    /// the etag rather than creating.
    private SegmentedReplayWriter resumingWriter() {
        service.info = info(ReplayState.RECORDING, ReplayRepresentation.SEGMENTED, 0);
        return storage.writer(ID, storage.load(ID));
    }

    private static ReplayInfo info(ReplayState state, ReplayRepresentation representation,
                                   @Nullable Integer nextSegmentIndex) {
        return new ReplayInfo(ID, 4, state, representation, nextSegmentIndex, PREAMBLE.length, null, 0);
    }

    private static byte[] concat(byte[] head, byte[] tail) {
        var out = new byte[head.length + tail.length];
        System.arraycopy(head, 0, out, 0, head.length);
        System.arraycopy(tail, 0, out, head.length, tail.length);
        return out;
    }

    private static String digest(byte[] body) {
        try {
            return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(body));
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    private static final class FakeService implements ReplayService {
        private final List<Sent> commits = new ArrayList<>();

        private @Nullable ReplayInfo info;
        private @Nullable Long preambleRevision;
        private int status = 200;
        private long version = 4;

        private record Sent(ReplayCommit meta, byte[] body) {
        }

        @Override
        public @Nullable ReplayInfo getReplay(String id) {
            return info;
        }

        @Override
        public Blob getPreamble(String id, @Nullable Long expectedRevision) {
            preambleRevision = expectedRevision;
            // An empty preamble reads as a recording with no chunks, so `nextSegmentIndex` is 0 and
            // anything else in the info is a disagreement.
            return Blob.of(emptyPreamble());
        }

        @Override
        public Blob getSegment(String id, int segmentIndex) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Blob getCompacted(String id, @Nullable Long start, @Nullable Long endInclusive) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int dropSegments(String id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ReplayInfo commit(ReplayCommit meta, Blob body) {
            try {
                commits.add(new Sent(meta, body.readAllBytes()));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            if (status != 200) throw new IpcException(status, "answered " + status);
            version++;
            return new ReplayInfo(meta.id(), version, ReplayState.RECORDING,
                ReplayRepresentation.SEGMENTED, 0, meta.preambleLength(), meta.outcome(), 0);
        }

        @Override
        public ReplayInfo publishCompacted(ReplayCompaction meta, Blob body) {
            throw new UnsupportedOperationException();
        }
    }

    /// A valid preamble with no chunks in it, which is what a recording looks like before its first
    /// segment is committed.
    private static byte[] emptyPreamble() {
        return NetworkBuffer.makeArray(buffer -> {
            var header = new ReplayHeader(new UUID(0, 0), new byte[0]);
            var metadata = NetworkBuffer.makeArray(NetworkBuffer.NBT_COMPOUND, CompoundBinaryTag.empty());
            header.update(metadata.length, 0, 0, 0);
            header.write(buffer);
            buffer.write(NetworkBuffer.RAW_BYTES, metadata);
        });
    }
}
