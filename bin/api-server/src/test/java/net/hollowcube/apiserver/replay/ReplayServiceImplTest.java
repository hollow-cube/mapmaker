package net.hollowcube.apiserver.replay;

import com.sun.net.httpserver.HttpServer;
import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.apiserver.db.Jobs;
import net.hollowcube.apiserver.db.ReplaysQueries;
import net.hollowcube.apiserver.job.CompactReplay;
import net.hollowcube.apiserver.job.JobSpec;
import net.hollowcube.apiserver.s3.MemoryS3Client;
import net.hollowcube.apiserver.s3.S3Client;
import net.hollowcube.ipc.Blob;
import net.hollowcube.ipc.replay.ReplayClient;
import net.hollowcube.ipc.replay.ReplayCommit;
import net.hollowcube.ipc.replay.ReplayCompaction;
import net.hollowcube.ipc.replay.ReplayInfo;
import net.hollowcube.ipc.replay.ReplayOutcome;
import net.hollowcube.ipc.replay.ReplayRepresentation;
import net.hollowcube.ipc.replay.ReplayServer;
import net.hollowcube.ipc.replay.ReplayState;
import net.hollowcube.ipc.util.IpcException;
import net.hollowcube.sqlgen.testing.TestDb;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Replay storage end to end: a real Postgres under the rows, a map under the bucket, and the
/// generated client over a real socket, because the bytes surviving the wire unchanged is the whole
/// point of the thing.
///
/// There is no concurrent-writer test: `TestDb` hands out one connection, so there is no second
/// session to race with.
class ReplayServiceImplTest {

    @RegisterExtension
    static final TestDb TEST_DB = TestDb.of("../../modules/api/src/main/sql/migrations");

    /// `ReplayServiceImpl`'s inline threshold, which the tests below straddle rather than lower.
    private static final int INLINE_BYTES = 2048;
    /// Its preamble ceiling, which is checked against the declared length before a byte is read.
    private static final int MAX_PREAMBLE_BYTES = 16 << 20;
    /// A segment one byte past the threshold, so it is staged as an object rather than inlined.
    private static final String EXTERNAL_SEGMENT = "x".repeat(INLINE_BYTES + 1);

    private static final String ID = "save-state-1";
    private static final byte[] PREAMBLE = "PREAMBLE".getBytes(StandardCharsets.UTF_8);

    private HttpServer server;
    private ApiDatabase db;
    private MemoryS3Client s3;
    private ReplayClient replays;
    private ReplayServiceImpl service;

    @BeforeEach
    void start() throws IOException {
        db = TEST_DB.database(ApiDatabase::new);
        s3 = new MemoryS3Client();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        service = new ReplayServiceImpl(db, s3);
        server.createContext(ReplayServer.PATH, new ReplayServer(service));
        server.start();

        replays = new ReplayClient(
            HttpClient.newHttpClient(),
            "http://127.0.0.1:" + server.getAddress().getPort()
        );
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    /// The whole life of a recording, and the etags it walks through on the way.
    @Test
    void commit_createsResumesAndFinishes() throws IOException {
        var created = commit(null, "k-0", 0, false, null, seg("aaa"));
        assertEquals(1, created.revision());
        assertEquals(ReplayState.RECORDING, created.state());
        assertEquals(ReplayRepresentation.SEGMENTED, created.representation());
        assertEquals(1, created.nextSegmentIndex());
        assertEquals(PREAMBLE.length, created.preambleLength());
        assertNull(created.outcome());

        var appended = commit(created.revision(), "k-1", 1, false, null, seg("bbbb"));
        assertEquals(2, appended.revision());
        assertEquals(2, appended.nextSegmentIndex());
        assertEquals(412, status(() -> replays.getPreamble(ID, created.revision())));

        var finished = commit(
            appended.revision(),
            "k-2",
            null,
            true,
            ReplayOutcome.RESET,
            new byte[0]
        );
        assertEquals(3, finished.revision());
        assertEquals(ReplayState.FINISHED, finished.state());
        assertEquals(ReplayOutcome.RESET, finished.outcome());
        assertEquals(2, finished.nextSegmentIndex());

        assertEquals(finished, replays.getReplay(ID));
        try (var preamble = replays.getPreamble(ID, finished.revision())) {
            assertArrayEquals(PREAMBLE, preamble.readAllBytes());
        }
        try (var preamble = replays.getPreamble(ID, null)) {
            assertArrayEquals(PREAMBLE, preamble.readAllBytes());
        }
        try (var segment = replays.getSegment(ID, 1)) {
            assertArrayEquals(seg("bbbb"), segment.readAllBytes());
        }
    }

    /// The inline threshold is inclusive. Nothing on the read path can tell which side it took.
    @ParameterizedTest
    @ValueSource(ints = {INLINE_BYTES, INLINE_BYTES + 1})
    void commit_keepsASegmentInlineUpToAndIncludingTheThreshold(int length) throws IOException {
        var segment = seg("x".repeat(length));

        commit(null, "k-0", 0, false, null, segment);

        var row = db.replays.getReplaySegment(ID, 0);
        assertNotNull(row);
        assertEquals(length, row.length());
        if (length <= INLINE_BYTES) {
            assertArrayEquals(segment, row.data());
            assertNull(row.objectReference());
            assertEquals(0, s3.puts());
        } else {
            assertNull(row.data());
            assertNotNull(row.objectReference());
            assertArrayEquals(segment, s3.objects().get(row.objectReference()));
        }

        try (var read = replays.getSegment(ID, 0)) {
            assertArrayEquals(segment, read.readAllBytes());
        }
    }

    @Test
    void commit_retriedWithTheSameKeyAndBodyAnswersTheRecordedResponseAndUploadsOnce() throws
        IOException {
        var segment = seg(EXTERNAL_SEGMENT);
        var first = commit(null, "k-0", 0, false, null, segment);

        var retry = commit(null, "k-0", 0, false, null, segment);

        assertEquals(first, retry);
        assertEquals(1, replays.getReplay(ID).revision());
        assertEquals(1, db.replays.listReplaySegmentObjects(ID).size());
        assertEquals(1, s3.puts());
        assertEquals(1, s3.objects().size());
    }

    @Test
    void commit_refusesAnythingButTheNextSegmentOfAnOpenRecording() throws IOException {
        assertConflict(
            "wrong_segment_index",
            () -> commit(null, "k-x", 3, false, null, seg("aaa"))
        );
        assertEquals(400, status(() -> commit(null, "k-x", null, false, null, new byte[0])));
        // The declared length is checked before a byte of the body is read.
        var oversized = new ReplayCommit(
            ID,
            null,
            "k-x",
            MAX_PREAMBLE_BYTES + 1,
            0,
            false,
            null,
            digest(PREAMBLE)
        );
        assertEquals(413, status(() -> replays.commit(oversized, Blob.of(PREAMBLE))));

        var created = commit(null, "k-0", 0, false, null, seg("aaa"));
        assertConflict(
            "idempotency_key_conflict",
            () -> commit(null, "k-0", 0, false, null, seg("bbb"))
        );
        assertConflict(
            "wrong_segment_index",
            () -> commit(created.revision(), "k-1", 4, false, null, seg("bbb"))
        );

        var finished = commit(
            created.revision(),
            "k-2",
            null,
            true,
            ReplayOutcome.FINISHED,
            new byte[0]
        );
        assertConflict(
            "replay_finished",
            () -> commit(finished.revision(), "k-3", 1, false, null, seg("bbb"))
        );
    }

    /// A rejection after a large segment was staged has to leave the bucket as it found it,
    /// because nothing ever collects what it does not.
    @Test
    void commit_withAStaleEtagIsAPreconditionFailureAndLeavesNoObject() throws IOException {
        var created = commit(null, "k-0", 0, false, null, seg("aaa"));
        commit(created.revision(), "k-1", 1, false, null, seg("bbb"));

        assertEquals(
            412,
            status(() -> commit(created.revision(), "k-2", 2, false, null, seg("ccc")))
        );
        assertEquals(412, status(() -> commit(null, "k-3", 0, false, null, seg("ccc"))));
        assertEquals(412, status(() -> commit(99L, "k-4", 2, false, null, seg(EXTERNAL_SEGMENT))));
        assertTrue(s3.objects().isEmpty(), s3.objects().keySet().toString());
    }

    /// Both storage paths, because it is the large one that has already written to the bucket by
    /// the time the digest can be checked.
    @ParameterizedTest
    @ValueSource(ints = {3, INLINE_BYTES + 1})
    void commit_aDigestMismatchPersistsNothingAndLeavesTheBucketEmpty(int length) {
        var segment = seg("z".repeat(length));
        var meta = new ReplayCommit(
            ID,
            null,
            "k-0",
            PREAMBLE.length,
            0,
            false,
            null,
            digest("something else".getBytes(StandardCharsets.UTF_8))
        );

        var failure = failure(() -> replays.commit(meta, Blob.of(concat(PREAMBLE, segment))));

        assertEquals(422, failure.status());
        assertTrue(failure.getMessage().contains("digest_mismatch"), failure.getMessage());
        assertNull(db.replays.getReplay(ID));
        assertTrue(s3.objects().isEmpty(), s3.objects().keySet().toString());
    }

    /// The row asking for the compaction commits with the commit that finished the replay, so the
    /// process dying in between cannot lose it.
    @Test
    void commit_thatFinishesTheReplayEnqueuesOneCompactionInTheSameTransaction() throws
        IOException {
        var created = commit(null, "k-0", 0, false, null, seg("aaa"));
        assertEquals(List.of(), compactionRows());

        commit(created.revision(), "k-1", null, true, ReplayOutcome.FINISHED, new byte[0]);
        commit(created.revision(), "k-1", null, true, ReplayOutcome.FINISHED, new byte[0]);

        assertEquals(List.of(ID), compactionRows());
        var row = db.jobs.listJobs()
            .stream()
            .filter(job -> job.job().equals(JobSpec.COMPACT_REPLAY.name()))
            .findFirst()
            .orElseThrow();
        assertEquals(
            new CompactReplay(ID, "final-commit"),
            JobSpec.COMPACT_REPLAY.decode(row.data())
        );
    }

    private List<String> compactionRows() {
        return db.jobs.listJobs()
            .stream()
            .filter(job -> job.job().equals(JobSpec.COMPACT_REPLAY.name()))
            .map(Jobs::instance)
            .toList();
    }

    @Test
    void publishCompacted_swapsTheRepresentationOnceAndReplacesThePreviousObject() throws
        IOException {
        var finished = finishedReplay();
        var compacted = "COMPACTEDPREAMBLE-and-its-chunks".getBytes(StandardCharsets.UTF_8);
        var meta = new ReplayCompaction(
            ID,
            finished.revision(),
            "compact:" + ID + ":" + finished.revision(),
            17,
            digest(compacted)
        );
        var puts = s3.puts();

        var published = replays.publishCompacted(meta, Blob.of(compacted));
        assertEquals(published, replays.publishCompacted(meta, Blob.of(compacted)));
        assertEquals(puts + 1, s3.puts());

        assertEquals(3, published.revision());
        assertEquals(ReplayRepresentation.COMPACTED, published.representation());
        assertEquals(ReplayState.FINISHED, published.state());
        assertNull(published.nextSegmentIndex());
        assertEquals(17, published.preambleLength());
        // Compaction does not change why the recording ended.
        assertEquals(ReplayOutcome.RESET, published.outcome());
        try (var stream = replays.getCompacted(ID, null, null)) {
            assertArrayEquals(compacted, stream.readAllBytes());
        }

        var replaced = db.replays.getReplay(ID).compactedObject();
        var transcoded = "COMPACTEDPREAMBLE-transcoded-chunks".getBytes(StandardCharsets.UTF_8);
        replays.publishCompacted(
            new ReplayCompaction(ID, published.revision(), "t-0", 17, digest(transcoded)),
            Blob.of(transcoded)
        );

        assertNull(s3.objects().get(replaced));
        try (var stream = replays.getCompacted(ID, null, null)) {
            assertArrayEquals(transcoded, stream.readAllBytes());
        }
    }

    @Test
    void compactedOperations_refuseAReplayThatIsStillRecording() throws IOException {
        var created = commit(null, "k-0", 0, false, null, seg(EXTERNAL_SEGMENT));
        var compacted = "COMPACTED".getBytes(StandardCharsets.UTF_8);

        assertConflict(
            "replay_not_finished",
            () -> replays.publishCompacted(
                new ReplayCompaction(ID, created.revision(), "c-0", 4, digest(compacted)),
                Blob.of(compacted)
            )
        );
        assertEquals(1, s3.puts());
        assertConflict("replay_not_compacted", () -> replays.getCompacted(ID, null, null));
        assertConflict("replay_not_compacted", () -> replays.dropSegments(ID));
        assertEquals(1, db.replays.listReplaySegmentObjects(ID).size());
        assertEquals(1, s3.objects().size());
    }

    @Test
    void dropSegmentsBefore_removesTheObjectsAndRowsBelowTheCut() {
        var first = commit(null, "k-0", 0, false, null, seg(EXTERNAL_SEGMENT));
        var dead = db.replays.getReplaySegment(ID, 0).objectReference();
        var second = commit(first.revision(), "k-1", 1, false, null, seg(EXTERNAL_SEGMENT));

        assertEquals(
            412,
            failure(() -> replays.dropSegmentsBefore(ID, first.revision(), 1)).status()
        );
        assertEquals(1, replays.dropSegmentsBefore(ID, second.revision(), 1));

        assertNull(s3.objects().get(dead));
        assertEquals(
            List.of(1L),
            db.replays.listReplaySegmentObjects(ID)
                .stream()
                .map(ReplaysQueries.ListReplaySegmentObjectsRow::segmentIndex)
                .toList()
        );
    }

    @Test
    void sweepOrphanedObjects_deletesOnlyOldObjectsNoReplayPointsAt() throws IOException {
        for (var notAShard : List.of("a", "abcde", "ABCD", "zz")) {
            assertEquals(400, status(() -> replays.sweepOrphanedObjects(notAShard, false)));
        }

        var created = commit(null, "k-0", 0, false, null, seg(EXTERNAL_SEGMENT));
        var segmentObject = db.replays.getReplaySegment(ID, 0).objectReference();
        var orphan = ReplayCompat.objectKey(ID, ReplayCompat.COMPACTED);
        var young = ReplayCompat.objectKey(ID, ReplayCompat.COMPACTED);
        s3.put(orphan, new ByteArrayInputStream(new byte[3]), 3);
        s3.put(young, new ByteArrayInputStream(new byte[3]), 3);
        var old = Instant.now().minus(Duration.ofDays(2));
        s3.lastModified(segmentObject, old);
        s3.lastModified(orphan, old);
        var shard = orphan.substring("replays/".length(), "replays/".length() + 4);

        assertEquals(1, replays.sweepOrphanedObjects(shard, false));
        assertNotNull(s3.objects().get(orphan));

        assertEquals(1, replays.sweepOrphanedObjects(shard, true));
        assertNull(s3.objects().get(orphan));
        assertNotNull(s3.objects().get(young));
        assertNotNull(s3.objects().get(segmentObject));
        assertEquals(created.revision(), replays.getReplay(ID).revision());
    }

    /// The sweep, which is where compaction's storage win is actually realised. Objects go first,
    /// so an interruption leaves rows pointing at nothing rather than objects nothing points at.
    @Test
    void dropSegments_removesTheObjectsAndTheRowsOfACompactedReplay() throws IOException {
        var created = commit(null, "k-0", 0, false, null, seg(EXTERNAL_SEGMENT));
        var finished = commit(
            created.revision(),
            "k-1",
            null,
            true,
            ReplayOutcome.RESET,
            new byte[0]
        );
        var compacted = "COMPACTEDPREAMBLE-and-its-chunks".getBytes(StandardCharsets.UTF_8);
        replays.publishCompacted(
            new ReplayCompaction(ID, finished.revision(), "c-0", 17, digest(compacted)),
            Blob.of(compacted)
        );
        var segmentObject = db.replays.getReplaySegment(ID, 0).objectReference();

        assertEquals(1, replays.dropSegments(ID));

        assertEquals(List.of(), db.replays.listReplaySegmentObjects(ID));
        assertNull(s3.objects().get(segmentObject));
        try (var stream = replays.getCompacted(ID, null, null)) {
            assertArrayEquals(compacted, stream.readAllBytes());
        }
    }

    @Test
    void getCompacted_readsOneRangeAndClampsItsEnd() throws IOException {
        compacted("0123456789".getBytes(StandardCharsets.UTF_8), 4);

        try (var stream = replays.getCompacted(ID, 2L, 5L)) {
            assertArrayEquals("2345".getBytes(StandardCharsets.UTF_8), stream.readAllBytes());
        }
        try (var stream = replays.getCompacted(ID, 6L, 200L)) {
            assertArrayEquals("6789".getBytes(StandardCharsets.UTF_8), stream.readAllBytes());
        }
        assertEquals(416, status(() -> replays.getCompacted(ID, 10L, 12L)));
        assertEquals(400, status(() -> replays.getCompacted(ID, 2L, null)));
    }

    /// Both handlers serve the same tables while both are up, so a recording the Go handler
    /// started is resumed here at the same etag and next index.
    @Test
    void storage_resumesARecordingWrittenByTheGoHandler() throws IOException {
        TEST_DB.seed(
            """
            insert into replays (id, version, recording_revision, state, representation,
                                 next_segment_index, current_preamble, current_preamble_digest)
            values ('%s', 4, 4, 'recording', 'segmented', 2, '\\x505245414d424c45',
                    decode('%s', 'hex'))
            """
                .formatted(ID, HexFormat.of().formatHex(sha256(PREAMBLE)))
        );

        var info = replays.getReplay(ID);
        assertNotNull(info);
        assertEquals(4, info.revision());
        assertEquals(2, info.nextSegmentIndex());
        assertNull(info.outcome());

        var appended = commit(info.revision(), "k-9", 2, false, null, seg(EXTERNAL_SEGMENT));
        assertEquals(5, appended.revision());
        assertEquals(3, appended.nextSegmentIndex());
        var segment = db.replays.getReplaySegment(ID, 2);
        assertEquals(5, segment.commitRevision());
        // The id is hashed rather than used, so a client-supplied one cannot leave the prefix.
        assertTrue(
            segment.objectReference().startsWith(
                "replays/" + HexFormat.of().formatHex(sha256(ID.getBytes(StandardCharsets.UTF_8)))
                    + "/segments/2/"
            ),
            segment.objectReference()
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void overlappingRetriesDiscardTheObjectTheyDidNotInstall(boolean compacted) throws IOException {
        var body = concat(PREAMBLE, seg(EXTERNAL_SEGMENT));
        var revision = compacted ? finishedReplay().revision() : 0;
        var commit = new ReplayCommit(
            ID,
            compacted ? revision : null,
            "overlap",
            PREAMBLE.length,
            0,
            false,
            null,
            digest(body)
        );
        var publication = new ReplayCompaction(
            ID,
            revision,
            "overlap",
            PREAMBLE.length,
            digest(body)
        );
        // Finish the other request after staging, before this request acquires the row lock.
        s3.afterPut(() -> {
            if (compacted) service.publishCompacted(publication, Blob.of(body));
            else service.commit(commit, Blob.of(body));
        });

        if (compacted) replays.publishCompacted(publication, Blob.of(body));
        else replays.commit(commit, Blob.of(body));

        assertEquals(2, s3.puts());
        var installed = compacted
            ? db.replays.getReplay(ID).compactedObject()
            : db.replays.getReplaySegment(ID, 0).objectReference();
        assertEquals(List.of(installed), List.copyOf(s3.objects().keySet()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void lostUploadResponsesDiscardTheStagedObject(boolean compacted) throws IOException {
        var body = concat(PREAMBLE, seg(EXTERNAL_SEGMENT));
        var revision = compacted ? finishedReplay().revision() : 0;
        s3.afterPut(() -> {
            throw new S3Client.RequestFailedError(
                "object was stored but the PUT response was lost"
            );
        });

        assertEquals(
            500,
            status(() -> {
                if (compacted)
                    replays.publishCompacted(
                        new ReplayCompaction(
                            ID,
                            revision,
                            "lost-upload",
                            PREAMBLE.length,
                            digest(body)
                        ),
                        Blob.of(body)
                    );
                else
                    replays.commit(
                        new ReplayCommit(
                            ID,
                            null,
                            "lost-upload",
                            PREAMBLE.length,
                            0,
                            false,
                            null,
                            digest(body)
                        ),
                        Blob.of(body)
                    );
            })
        );
        assertTrue(s3.objects().isEmpty());
        assertNull(db.replays.getReplayIdempotency(ID, "lost-upload"));
    }

    private ReplayInfo finishedReplay() throws IOException {
        var created = commit(null, "k-0", 0, false, null, seg("aaa"));
        return commit(created.revision(), "k-1", null, true, ReplayOutcome.RESET, new byte[0]);
    }

    private void compacted(byte[] body, int preambleLength) throws IOException {
        var finished = finishedReplay();
        replays.publishCompacted(
            new ReplayCompaction(ID, finished.revision(), "c-0", preambleLength, digest(body)),
            Blob.of(body)
        );
    }

    private ReplayInfo commit(
        Long revision,
        String key,
        Integer segmentIndex,
        boolean finished,
        ReplayOutcome outcome,
        byte[] segment
    ) {
        var body = concat(PREAMBLE, segment);
        return replays.commit(
            new ReplayCommit(
                ID,
                revision,
                key,
                PREAMBLE.length,
                segmentIndex,
                finished,
                outcome,
                digest(body)
            ),
            Blob.of(body)
        );
    }

    private static byte[] seg(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] concat(byte[] head, byte[] tail) {
        var out = new byte[head.length + tail.length];
        System.arraycopy(head, 0, out, 0, head.length);
        System.arraycopy(tail, 0, out, head.length, tail.length);
        return out;
    }

    private static String digest(byte[] body) {
        return Base64.getEncoder().encodeToString(sha256(body));
    }

    private static byte[] sha256(byte[] body) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(body);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    private static void assertConflict(String reason, Executable call) {
        var failure = failure(call);
        assertEquals(409, failure.status());
        assertTrue(failure.getMessage().contains(reason), failure.getMessage());
    }

    private static int status(Executable call) {
        return failure(call).status();
    }

    private static IpcException failure(Executable call) {
        return assertThrows(IpcException.class, call);
    }
}
