package net.hollowcube.apiserver.replay;

import com.sun.net.httpserver.HttpServer;
import net.hollowcube.apiserver.s3.S3Client;
import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.apiserver.db.Jobs;
import net.hollowcube.apiserver.s3.MemoryS3Client;
import net.hollowcube.apiserver.job.CompactReplay;
import net.hollowcube.apiserver.job.JobSpec;
import net.hollowcube.ipc.Blob;
import net.hollowcube.ipc.replay.ReplayClient;
import net.hollowcube.ipc.replay.ReplayCommit;
import net.hollowcube.ipc.replay.ReplayInfo;
import net.hollowcube.ipc.replay.ReplayCompaction;
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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
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
/// The Go handler's test matrix is ported wholesale, since the two serve the same tables and a
/// recording started on one is resumed on the other. What is not ported is its concurrent-writer
/// test: `TestDb` hands out one connection, so there is no second session to race with.
class ReplayServiceImplTest {

    @RegisterExtension
    // TRUNCATE rather than ROLLBACK: every write here goes through `db.txResult`, which cannot
    // commit inside a transaction the harness is holding open.
    static final TestDb TEST_DB = TestDb.of("../../modules/api/src/main/sql/migrations", TestDb.Mode.TRUNCATE);

    /// `ReplayServiceImpl`'s inline threshold, which the tests below straddle rather than lower.
    private static final int INLINE_BYTES = 2048;
    /// Its preamble ceiling, which is checked against the declared length before a byte is read.
    private static final int MAX_PREAMBLE_BYTES = 16 << 20;
    /// A segment one byte past the threshold, so it is staged as an object rather than inlined —
    /// which is the only path with anything to orphan.
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

        replays = new ReplayClient(HttpClient.newHttpClient(),
            "http://127.0.0.1:" + server.getAddress().getPort());
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void migration_leavesTheTablesTheQueriesWereDescribedAgainst() {
        assertNull(db.replays.getReplay(ID));
    }

    /// `select replay_segments.*` is read by column position, so the mirror has to leave the columns
    /// in the order production has them — Go's `000034` followed by the `data` its `000035` appends,
    /// not the tidier order the same columns take when declared in one statement. Flattening the two
    /// put `data` in the middle here and at the end in production, so every segment read answered
    /// 500 against a real database while every test here passed.
    @Test
    void migration_ordersReplaySegmentsAsProductionHasThem() {
        assertEquals(
            List.of("replay_id", "segment_index", "object_reference", "length", "digest",
                "commit_revision", "data"),
            columnsOf("replay_segments"));
    }

    /// `pg_attribute` rather than `information_schema`, which pglite does not serve.
    private static List<String> columnsOf(String table) {
        var sql = """
            select a.attname
            from pg_attribute a
                     join pg_class c on c.oid = a.attrelid
                     join pg_namespace n on n.oid = c.relnamespace
            where n.nspname = 'public' and c.relname = '%s'
              and a.attnum > 0 and not a.attisdropped
            order by a.attnum""".formatted(table);
        try (var statement = TEST_DB.conn().createStatement();
             var rows = statement.executeQuery(sql)) {
            var columns = new ArrayList<String>();
            while (rows.next()) columns.add(rows.getString(1));
            return columns;
        } catch (SQLException e) {
            throw new IllegalStateException("could not read the columns of " + table, e);
        }
    }

    @Test
    void getReplay_isNullForOneNothingHasCommitted() {
        assertNull(replays.getReplay("never-recorded"));
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

        // The metadata-only final commit: no segment, and the outcome the recorder says it had.
        var finished = commit(appended.revision(), "k-2", null, true, ReplayOutcome.RESET, new byte[0]);
        assertEquals(3, finished.revision());
        assertEquals(ReplayState.FINISHED, finished.state());
        assertEquals(ReplayOutcome.RESET, finished.outcome());
        assertEquals(2, finished.nextSegmentIndex());

        assertEquals(finished, replays.getReplay(ID));
        try (var preamble = replays.getPreamble(ID, finished.revision())) {
            assertArrayEquals(PREAMBLE, preamble.readAllBytes());
        }
        try (var segment = replays.getSegment(ID, 1)) {
            assertArrayEquals(seg("bbbb"), segment.readAllBytes());
        }
    }

    @Test
    void getPreamble_refusesAnEtagTheReplayHasMovedOnFrom() throws IOException {
        var created = commit(null, "k-0", 0, false, null, seg("aaa"));
        commit(created.revision(), "k-1", 1, false, null, seg("bbb"));

        assertEquals(412, status(() -> replays.getPreamble(ID, created.revision())));
        // No condition at all is still a read.
        try (var preamble = replays.getPreamble(ID, null)) {
            assertArrayEquals(PREAMBLE, preamble.readAllBytes());
        }
    }

    /// The inline threshold is inclusive: with a limit of 8, eight bytes stay in the row and nine
    /// become an object. Nothing on the read path can tell which.
    @ParameterizedTest
    @ValueSource(ints = {INLINE_BYTES - 1, INLINE_BYTES, INLINE_BYTES + 1})
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
    void commit_retriedWithTheSameKeyAndBodyAnswersTheRecordedResponse() throws IOException {
        var first = commit(null, "k-0", 0, false, null, seg("aaa"));

        var retry = commit(null, "k-0", 0, false, null, seg("aaa"));

        assertEquals(first, retry);
        assertEquals(1, db.replays.listReplaySegmentObjects(ID).size());
        assertEquals(1, replays.getReplay(ID).revision());
    }

    /// The retry that Go turns into a second full upload and then throws away.
    @Test
    void commit_retriedWithALargeSegmentUploadsNothingASecondTime() throws IOException {
        var segment = seg(EXTERNAL_SEGMENT);
        var first = commit(null, "k-0", 0, false, null, segment);
        assertEquals(1, s3.puts());

        var retry = commit(null, "k-0", 0, false, null, segment);

        assertEquals(first, retry);
        assertEquals(1, s3.puts());
        assertEquals(1, s3.objects().size());
    }

    @Test
    void commit_sameKeyDifferentBodyIsAConflictRatherThanAReplayedSuccess() throws IOException {
        commit(null, "k-0", 0, false, null, seg("aaa"));

        var failure = failure(() -> commit(null, "k-0", 0, false, null, seg("bbb")));
        assertEquals(409, failure.status());
        assertTrue(failure.getMessage().contains("idempotency_key_conflict"), failure.getMessage());
    }

    @Test
    void commit_withAStaleEtagIsAPreconditionFailure() throws IOException {
        var created = commit(null, "k-0", 0, false, null, seg("aaa"));
        commit(created.revision(), "k-1", 1, false, null, seg("bbb"));

        assertEquals(412, status(() -> commit(created.revision(), "k-2", 2, false, null, seg("ccc"))));
    }

    @Test
    void commit_creatingOverAReplayThatExistsIsAPreconditionFailure() throws IOException {
        commit(null, "k-0", 0, false, null, seg("aaa"));

        assertEquals(412, status(() -> commit(null, "k-1", 1, false, null, seg("bbb"))));
    }

    @Test
    void commit_afterTheFinalOneIsRefusedForever() throws IOException {
        var finished = commit(null, "k-0", 0, true, ReplayOutcome.FINISHED, seg("aaa"));

        var failure = failure(() -> commit(finished.revision(), "k-1", 1, false, null, seg("bbb")));
        assertEquals(409, failure.status());
        assertTrue(failure.getMessage().contains("replay_finished"), failure.getMessage());
    }

    @Test
    void commit_outOfOrderSegmentIsAConflict() throws IOException {
        var created = commit(null, "k-0", 0, false, null, seg("aaa"));

        var failure = failure(() -> commit(created.revision(), "k-1", 4, false, null, seg("bbb")));
        assertEquals(409, failure.status());
        assertTrue(failure.getMessage().contains("wrong_segment_index"), failure.getMessage());
    }

    @Test
    void commit_firstSegmentOfANewReplayMustBeZero() {
        var failure = failure(() -> commit(null, "k-0", 3, false, null, seg("aaa")));
        assertEquals(409, failure.status());
        assertTrue(failure.getMessage().contains("wrong_segment_index"), failure.getMessage());
    }

    @Test
    void commit_withNoSegmentMustFinishTheReplay() {
        assertEquals(400, status(() -> commit(null, "k-0", null, false, null, new byte[0])));
    }

    /// Both storage paths, because it is the large one that has already written to the bucket by
    /// the time the digest can be checked.
    @ParameterizedTest
    @ValueSource(ints = {3, INLINE_BYTES + 1})
    void commit_aDigestMismatchPersistsNothingAndLeavesTheBucketEmpty(int length) {
        var segment = seg("z".repeat(length));
        var meta = new ReplayCommit(ID, null, "k-0", PREAMBLE.length, 0, false, null,
            digest("something else".getBytes(StandardCharsets.UTF_8)));

        var failure = failure(() -> replays.commit(meta, Blob.of(concat(PREAMBLE, segment))));

        assertEquals(422, failure.status());
        assertTrue(failure.getMessage().contains("digest_mismatch"), failure.getMessage());
        assertNull(db.replays.getReplay(ID));
        assertTrue(s3.objects().isEmpty(), s3.objects().keySet().toString());
    }

    /// Go stages the object first and drops it on the floor when the row says no. A rejection here
    /// has to leave the bucket as it found it, because nothing ever collects what it does not.
    @Test
    void commit_rejectedAfterALargeSegmentWasStagedLeavesNoObject() throws IOException {
        var created = commit(null, "k-0", 0, false, null, seg("aaa"));
        var objects = s3.objects().size();

        assertEquals(412, status(() -> commit(99L, "k-1", 1, false, null, seg(EXTERNAL_SEGMENT))));

        assertEquals(objects, s3.objects().size());
    }

    /// The declared length is what is checked, before a byte of the body is read — which is also
    /// what stops a caller asking this process to allocate sixteen megabytes it will never fill.
    @Test
    void commit_refusesAPreambleOverTheLimit() {
        var meta = new ReplayCommit(ID, null, "k-0", MAX_PREAMBLE_BYTES + 1, 0, false, null,
            digest(PREAMBLE));

        assertEquals(413, status(() -> replays.commit(meta, Blob.of(PREAMBLE))));
    }

    /// The first production transactional enqueue: the row asking for the compaction commits with
    /// the commit that finished the replay, so the process dying in between cannot lose it.
    @Test
    void commit_thatFinishesTheReplayEnqueuesItsCompactionInTheSameTransaction() throws IOException {
        var created = commit(null, "k-0", 0, false, null, seg("aaa"));
        assertEquals(List.of(), compactionRows());

        commit(created.revision(), "k-1", null, true, ReplayOutcome.FINISHED, new byte[0]);

        assertEquals(List.of(ID), compactionRows());
        var row = db.jobs.listJobs().stream()
            .filter(job -> job.job().equals(JobSpec.COMPACT_REPLAY.name())).findFirst().orElseThrow();
        assertEquals(new CompactReplay(ID, "final-commit"), JobSpec.COMPACT_REPLAY.decode(row.data()));
    }

    @Test
    void commit_thatFinishesTheReplayTwiceEnqueuesOneRow() throws IOException {
        var created = commit(null, "k-0", 0, false, null, seg("aaa"));

        commit(created.revision(), "k-1", null, true, ReplayOutcome.FINISHED, new byte[0]);
        commit(created.revision(), "k-1", null, true, ReplayOutcome.FINISHED, new byte[0]);

        assertEquals(List.of(ID), compactionRows());
    }

    private List<String> compactionRows() {
        return db.jobs.listJobs().stream()
            .filter(job -> job.job().equals(JobSpec.COMPACT_REPLAY.name()))
            .map(Jobs::instance)
            .toList();
    }

    @Test
    void publishCompacted_swapsTheRepresentationAndDropsTheNextSegmentIndex() throws IOException {
        var finished = finishedReplay();
        var compacted = "COMPACTEDPREAMBLE-and-its-chunks".getBytes(StandardCharsets.UTF_8);

        var published = replays.publishCompacted(
            new ReplayCompaction(ID, finished.revision(), "compact:" + ID + ":" + finished.revision(), 17, digest(compacted)),
            Blob.of(compacted));

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
    }

    @Test
    void publishCompacted_overAReplayStillRecordingIsAConflict() throws IOException {
        var created = commit(null, "k-0", 0, false, null, seg("aaa"));
        var compacted = "COMPACTED".getBytes(StandardCharsets.UTF_8);

        var failure = failure(() -> replays.publishCompacted(
            new ReplayCompaction(ID, created.revision(), "c-0", 4, digest(compacted)), Blob.of(compacted)));

        assertEquals(409, failure.status());
        assertTrue(failure.getMessage().contains("replay_not_finished"), failure.getMessage());
        // Go uploads the whole thing before it ever looks at the row.
        assertEquals(0, s3.puts());
    }

    @Test
    void publishCompacted_retriedWithTheDeterministicKeyUploadsOnce() throws IOException {
        var finished = finishedReplay();
        var compacted = "COMPACTEDPREAMBLE-and-its-chunks".getBytes(StandardCharsets.UTF_8);
        var meta = new ReplayCompaction(ID, finished.revision(), "compact:" + ID + ":" + finished.revision(), 17, digest(compacted));
        var puts = s3.puts();

        var first = replays.publishCompacted(meta, Blob.of(compacted));
        var retry = replays.publishCompacted(meta, Blob.of(compacted));

        assertEquals(first, retry);
        assertEquals(puts + 1, s3.puts());
    }

    /// The sweep, which is where compaction's storage win is actually realised. Objects go first,
    /// so an interruption leaves rows pointing at nothing rather than objects nothing points at.
    @Test
    void dropSegments_removesTheObjectsAndTheRowsOfACompactedReplay() throws IOException {
        var created = commit(null, "k-0", 0, false, null, seg(EXTERNAL_SEGMENT));
        var finished = commit(created.revision(), "k-1", null, true, ReplayOutcome.RESET, new byte[0]);
        var compacted = "COMPACTEDPREAMBLE-and-its-chunks".getBytes(StandardCharsets.UTF_8);
        replays.publishCompacted(
            new ReplayCompaction(ID, finished.revision(), "c-0", 17, digest(compacted)), Blob.of(compacted));
        var segmentObject = db.replays.getReplaySegment(ID, 0).objectReference();

        assertEquals(1, replays.dropSegments(ID));

        assertEquals(List.of(), db.replays.listReplaySegmentObjects(ID));
        assertNull(s3.objects().get(segmentObject));
        // The compacted object is what the replay now is, and is not a source.
        try (var stream = replays.getCompacted(ID, null, null)) {
            assertArrayEquals(compacted, stream.readAllBytes());
        }
    }

    @Test
    void dropSegments_refusesAReplayThatIsStillSegmented() throws IOException {
        commit(null, "k-0", 0, false, null, seg(EXTERNAL_SEGMENT));

        var failure = failure(() -> replays.dropSegments(ID));
        assertEquals(409, failure.status());
        assertTrue(failure.getMessage().contains("replay_not_compacted"), failure.getMessage());
        assertEquals(1, db.replays.listReplaySegmentObjects(ID).size());
        assertEquals(1, s3.objects().size());
    }

    @Test
    void getCompacted_isAConflictWhileTheReplayIsStillSegmented() throws IOException {
        commit(null, "k-0", 0, false, null, seg("aaa"));

        var failure = failure(() -> replays.getCompacted(ID, null, null));
        assertEquals(409, failure.status());
        assertTrue(failure.getMessage().contains("replay_not_compacted"), failure.getMessage());
    }

    @Test
    void getCompacted_readsOneRangeAndClampsItsEnd() throws IOException {
        var compacted = "0123456789".getBytes(StandardCharsets.UTF_8);
        compacted(compacted, 4);

        try (var stream = replays.getCompacted(ID, 2L, 5L)) {
            assertArrayEquals("2345".getBytes(StandardCharsets.UTF_8), stream.readAllBytes());
        }
        // Past the end is clamped rather than refused, exactly as a Range header is.
        try (var stream = replays.getCompacted(ID, 6L, 200L)) {
            assertArrayEquals("6789".getBytes(StandardCharsets.UTF_8), stream.readAllBytes());
        }
    }

    @Test
    void getCompacted_refusesARangeThatStartsPastTheEnd() throws IOException {
        compacted("0123456789".getBytes(StandardCharsets.UTF_8), 4);

        assertEquals(416, status(() -> replays.getCompacted(ID, 10L, 12L)));
        assertEquals(416, status(() -> replays.getCompacted(ID, 200L, 300L)));
        // One bound is not a range.
        assertEquals(400, status(() -> replays.getCompacted(ID, 2L, null)));
    }

    /// The invariants of §5.1: a row written here has to be one the Go handler would have written,
    /// because both are writing into the same table while both are up.
    @Test
    void storage_writesRowsAndKeysTheGoHandlerWouldHaveWritten() throws IOException {
        var created = commit(null, "k-0", 0, false, null, seg(EXTERNAL_SEGMENT));
        var finished = commit(created.revision(), "k-1", null, true, ReplayOutcome.FINISHED, new byte[0]);

        var row = db.replays.getReplay(ID);
        assertNotNull(row);
        assertEquals(row.version(), finished.revision());
        assertEquals(2, row.version());
        // Only a commit that appended moves the recording revision, which is what a segment's
        // commit_revision points back at.
        assertEquals(2, row.recordingRevision());
        assertEquals("finished", row.state());
        assertEquals("segmented", row.representation());
        assertEquals(1, row.nextSegmentIndex());
        assertEquals(32, row.currentPreambleDigest().length);
        assertArrayEquals(sha256(PREAMBLE), row.currentPreambleDigest());
        assertNull(row.compactedObject());
        assertEquals("finished", row.outcome());

        var segment = db.replays.getReplaySegment(ID, 0);
        assertNotNull(segment);
        assertEquals(1, segment.commitRevision());
        assertEquals(32, segment.digest().length);
        assertArrayEquals(sha256(seg(EXTERNAL_SEGMENT)), segment.digest());
        assertTrue(segment.objectReference().matches(
            "replays/[0-9a-f]{64}/segments/0/[0-9a-f-]{36}"), segment.objectReference());
        // The id is hashed rather than used, so a client-supplied one cannot leave the prefix.
        assertTrue(segment.objectReference().startsWith(
            "replays/" + HexFormat.of().formatHex(sha256(ID.getBytes(StandardCharsets.UTF_8))) + "/"));
    }

    /// A recording the Go handler wrote, resumed here. Same row, same etag, same next index.
    @Test
    void storage_resumesARecordingWrittenByTheGoHandler() throws IOException {
        TEST_DB.seed("""
            insert into replays (id, version, recording_revision, state, representation,
                                 next_segment_index, current_preamble, current_preamble_digest)
            values ('%s', 4, 4, 'recording', 'segmented', 2, '\\x505245414d424c45',
                    decode('%s', 'hex'))
            """.formatted(ID, HexFormat.of().formatHex(sha256(PREAMBLE))));

        var info = replays.getReplay(ID);
        assertNotNull(info);
        assertEquals(4, info.revision());
        assertEquals(2, info.nextSegmentIndex());
        assertNull(info.outcome());

        var appended = commit(info.revision(), "k-9", 2, false, null, seg("ccc"));
        assertEquals(5, appended.revision());
        assertEquals(3, appended.nextSegmentIndex());
        assertEquals(5, db.replays.getReplaySegment(ID, 2).commitRevision());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void overlappingRetriesDiscardTheObjectTheyDidNotInstall(boolean compacted) throws IOException {
        var body = concat(PREAMBLE, seg(EXTERNAL_SEGMENT));
        var revision = compacted ? finishedReplay().revision() : 0;
        var commit = new ReplayCommit(ID, compacted ? revision : null, "overlap", PREAMBLE.length,
            0, false, null, digest(body));
        var publication = new ReplayCompaction(ID, revision, "overlap", PREAMBLE.length, digest(body));
        // Finish the other request after staging, before this request acquires the row lock.
        s3.afterPut(() -> {
            if (compacted) service.publishCompacted(publication, Blob.of(body));
            else service.commit(commit, Blob.of(body));
        });

        if (compacted) replays.publishCompacted(publication, Blob.of(body));
        else replays.commit(commit, Blob.of(body));

        assertEquals(2, s3.puts());
        var installed = compacted ? db.replays.getReplay(ID).compactedObject()
            : db.replays.getReplaySegment(ID, 0).objectReference();
        assertEquals(List.of(installed), List.copyOf(s3.objects().keySet()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void lostUploadResponsesDiscardTheStagedObject(boolean compacted) throws IOException {
        var body = concat(PREAMBLE, seg(EXTERNAL_SEGMENT));
        var revision = compacted ? finishedReplay().revision() : 0;
        s3.afterPut(() -> {
            throw new S3Client.RequestFailedError("object was stored but the PUT response was lost");
        });

        assertEquals(500, status(() -> {
            if (compacted) replays.publishCompacted(
                new ReplayCompaction(ID, revision, "lost-upload", PREAMBLE.length, digest(body)), Blob.of(body));
            else replays.commit(
                new ReplayCommit(ID, null, "lost-upload", PREAMBLE.length, 0, false, null, digest(body)), Blob.of(body));
        }));
        assertTrue(s3.objects().isEmpty());
        assertNull(db.replays.getReplayIdempotency(ID, "lost-upload"));
    }

    @Test
    void storageFingerprintUsesUtf8ByteLengths() {
        // Go writeFingerprintField uses len(string), which counts the UTF-8 bytes of this ID.
        assertEquals("c9d781491eb9a4804f5b6db760931b95bb8de7a666faa3f2ca7125733fc16d61",
            HexFormat.of().formatHex(ReplayCompat.fingerprint("PATCH", "réplay", "", "*", "8", "0", "false",
                sha256(concat(PREAMBLE, seg("aaa"))))));
    }

    private ReplayInfo finishedReplay() throws IOException {
        var created = commit(null, "k-0", 0, false, null, seg("aaa"));
        return commit(created.revision(), "k-1", null, true, ReplayOutcome.RESET, new byte[0]);
    }

    private void compacted(byte[] body, int preambleLength) throws IOException {
        var finished = finishedReplay();
        replays.publishCompacted(
            new ReplayCompaction(ID, finished.revision(), "c-0", preambleLength, digest(body)), Blob.of(body));
    }

    private ReplayInfo commit(Long revision, String key, Integer segmentIndex,
                                                        boolean finished, ReplayOutcome outcome, byte[] segment) {
        var body = concat(PREAMBLE, segment);
        return replays.commit(
            new ReplayCommit(ID, revision, key, PREAMBLE.length, segmentIndex, finished, outcome, digest(body)),
            Blob.of(body));
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

    private static int status(Executable call) {
        return failure(call).status();
    }

    private static IpcException failure(Executable call) {
        return assertThrows(IpcException.class, call);
    }
}
