package net.hollowcube.apiserver.anticheat;

import com.sun.net.httpserver.HttpServer;
import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.ipc.Blob;
import net.hollowcube.ipc.anticheat.AnticheatClient;
import net.hollowcube.ipc.anticheat.AnticheatServer;
import net.hollowcube.ipc.anticheat.TraceMeta;
import net.hollowcube.ipc.util.IpcException;
import net.hollowcube.sqlgen.testing.TestDb;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The trace store end to end: a real Postgres under the index, a real directory under the blobs,
/// and the generated client over a real socket, because the bytes surviving the wire unchanged is
/// the whole point of the thing.
class AnticheatServiceImplTest {

    @RegisterExtension
    // The schema lives with the queries in modules/api; this is the service on top of it.
    static final TestDb TEST_DB = TestDb.of("../../modules/api/src/main/sql/migrations");

    private static final String PLAYER = "11111111-1111-1111-1111-111111111111";
    private static final String CAPTURE = "run-7";
    private static final long STARTED = Instant.parse("2026-08-29T22:15:30Z").toEpochMilli();
    /// Small enough that the too-large case is one read of a body already sent, not a stream to kill.
    private static final long MAX_BYTES = 1 << 20;

    @TempDir
    Path root;

    private HttpServer server;
    private ApiDatabase db;
    private AnticheatClient traces;

    @BeforeEach
    void start() throws IOException {
        this.db = TEST_DB.database(ApiDatabase::new);
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.server.createContext(AnticheatServer.PATH, new AnticheatServer(
            new AnticheatServiceImpl(this.db, new AnticheatTraceStore(this.root, MAX_BYTES))));
        this.server.start();

        this.traces = new AnticheatClient(HttpClient.newHttpClient(),
            "http://127.0.0.1:" + this.server.getAddress().getPort());
    }

    @AfterEach
    void stop() {
        this.server.stop(0);
    }

    @Test
    void migration_leavesTheTableTheQueriesWereDescribedAgainst() {
        assertEquals(List.of(), this.db.anticheat.deleteExpiredAnticheatTraces());
    }

    @Test
    void put_storesTheBlobAndIndexesItOffTheMetaTheProxySent() throws IOException {
        var blob = trace(1, "frames");

        var result = this.traces.putTrace(meta("seg-1", CAPTURE, STARTED), Blob.of(blob));

        assertFalse(result.replaced());
        // Dated by when the capture started, so a ship retried past midnight names the same file.
        assertEquals("2026/08/29/seg-1.trace", result.path());
        assertEquals(blob.length, result.bytes());

        var row = this.db.anticheat.getAnticheatTrace("seg-1");
        assertNotNull(row);
        assertEquals(CAPTURE, row.captureId());
        assertEquals(PLAYER, row.playerId().toString());
        assertEquals("abc1234", row.proxyVersion());
        assertEquals("proxy-a", row.proxy());
        assertEquals(776, row.clientPvn());
        assertEquals("run", row.reason());
        // Off the bytes, rather than off what the meta claimed about them.
        assertEquals(1, row.formatVersion());
        assertEquals(blob.length, row.bytes());
        assertFalse(row.pinned());
        assertNull(row.expiresAt());
        assertNull(row.endedAt());
        assertEquals("2026/08/29/seg-1.trace", row.path());
        assertArrayEquals(blob, Files.readAllBytes(this.root.resolve(row.path())));
    }

    /// A trace is megabytes of frames, so it is streamed both ways; what comes back has to be
    /// what went out, byte for byte and not one read of it.
    @Test
    void get_answersTheExactBytesThatWerePut() throws IOException {
        var blob = trace(1, "a capture, ".repeat(50_000));
        this.traces.putTrace(meta("seg-1", CAPTURE, STARTED), Blob.of(blob));

        try (var answer = this.traces.getTrace("seg-1")) {
            assertEquals(blob.length, answer.length());
            assertArrayEquals(blob, answer.stream().readAllBytes());
        }
    }

    @Test
    void get_is404ForATraceNobodyStored() {
        var thrown = assertThrows(IpcException.class, () -> this.traces.getTrace("seg-nope"));

        assertEquals(404, thrown.status());
    }

    @Test
    void list_answersEveryTraceOfOneCaptureOldestFirst() {
        this.traces.putTrace(meta("seg-2", CAPTURE, STARTED + 300_000), Blob.of(trace(1, "second")));
        this.traces.putTrace(meta("seg-1", CAPTURE, STARTED), Blob.of(trace(1, "first")));
        this.traces.putTrace(meta("seg-3", "run-8", STARTED), Blob.of(trace(1, "another capture")));

        var rows = this.traces.listTraces(CAPTURE);

        assertEquals(List.of("seg-1", "seg-2"), rows.stream().map(row -> row.meta().id()).toList());
        assertEquals(PLAYER, rows.getFirst().meta().playerId());
        assertEquals(STARTED, rows.getFirst().meta().startedAt());
        assertEquals("2026/08/29/seg-1.trace", rows.getFirst().path());

        assertEquals(1, this.traces.listTraces("run-8").size());
        assertEquals(List.of(), this.traces.listTraces("run-nothing"));
    }

    @Test
    void put_upsertsTheRowWhenTheProxyRetriesTheSameTrace() throws IOException {
        assertFalse(this.traces.putTrace(meta("seg-1", CAPTURE, STARTED), Blob.of(trace(1, "partial"))).replaced());

        // The retry ships the closed trace: same id, same path, a longer body and an ended_at.
        var closed = trace(1, "partial and then some");
        var ended = Instant.parse("2026-08-29T22:16:00Z").toEpochMilli();
        var meta = new TraceMeta("seg-1", CAPTURE, PLAYER, "abc1234", "proxy-a", 776, "run", STARTED, ended, 1);

        assertTrue(this.traces.putTrace(meta, Blob.of(closed)).replaced());

        var row = this.db.anticheat.getAnticheatTrace("seg-1");
        assertNotNull(row);
        assertEquals(closed.length, row.bytes());
        assertEquals("2026-08-29T22:16:00Z", String.valueOf(row.endedAt()));
        assertArrayEquals(closed, Files.readAllBytes(this.root.resolve(row.path())));
        assertEquals(1, this.traces.listTraces(CAPTURE).size());
    }

    @Test
    void put_refusesABodyThatDoesNotOpenWithTheMagic() {
        var body = Blob.of("PKnot a trace".getBytes(StandardCharsets.UTF_8));

        var thrown = assertThrows(IpcException.class, () -> this.traces.putTrace(meta("seg-1", CAPTURE, STARTED), body));

        assertEquals(400, thrown.status());
        assertNull(this.db.anticheat.getAnticheatTrace("seg-1"));
    }

    @Test
    void put_refusesATraceLongerThanTheStoreAccepts() {
        var body = Blob.of(trace(1, "x".repeat((int) MAX_BYTES)));

        var thrown = assertThrows(IpcException.class, () -> this.traces.putTrace(meta("seg-1", CAPTURE, STARTED), body));

        assertEquals(413, thrown.status());
        assertNull(this.db.anticheat.getAnticheatTrace("seg-1"));
    }

    @Test
    void put_refusesWhatTheTableWouldRefuse() {
        var body = trace(1, "frames");

        assertEquals(400, assertThrows(IpcException.class,
            () -> this.traces.putTrace(meta(".hidden", CAPTURE, STARTED), Blob.of(body))).status());
        assertEquals(400, assertThrows(IpcException.class,
            () -> this.traces.putTrace(meta("seg!1", CAPTURE, STARTED), Blob.of(body))).status());

        var reason = new TraceMeta("seg-1", CAPTURE, PLAYER, null, null, 0, "whatever", STARTED, null, 1);
        assertEquals(400, assertThrows(IpcException.class,
            () -> this.traces.putTrace(reason, Blob.of(body))).status());

        var player = new TraceMeta("seg-1", CAPTURE, "nope", null, null, 0, "run", STARTED, null, 1);
        assertEquals(400, assertThrows(IpcException.class,
            () -> this.traces.putTrace(player, Blob.of(body))).status());

        assertNull(this.db.anticheat.getAnticheatTrace("seg-1"));
    }

    /// A blob is renamed into place or not written at all, so a refusal leaves nothing behind —
    /// including the temp name it was streamed onto.
    @Test
    void put_leavesNoHalfWrittenFileBehindWhateverItDecided() throws IOException {
        this.traces.putTrace(meta("seg-1", CAPTURE, STARTED), Blob.of(trace(1, "frames")));
        var junk = Blob.of("junk".getBytes(StandardCharsets.UTF_8));
        assertThrows(IpcException.class, () -> this.traces.putTrace(meta("seg-2", CAPTURE, STARTED), junk));
        var big = Blob.of(trace(1, "x".repeat((int) MAX_BYTES)));
        assertThrows(IpcException.class, () -> this.traces.putTrace(meta("seg-3", CAPTURE, STARTED), big));

        try (Stream<Path> files = Files.walk(this.root)) {
            assertEquals(List.of("seg-1.trace"), files.filter(Files::isRegularFile)
                .map(file -> file.getFileName().toString()).sorted().toList());
        }
    }

    /// The meta of a blob call travels in a header rather than the body, which is bytes; a proxy
    /// named outside ascii still has to come back out as itself.
    @Test
    void put_carriesMetaWrittenOutsideAsciiThroughItsHeader() {
        var meta = new TraceMeta("seg-1", CAPTURE, PLAYER, "abc1234", "pröxy-å", 776, "run", STARTED, null, 1);

        this.traces.putTrace(meta, Blob.of(trace(1, "frames")));

        var row = this.db.anticheat.getAnticheatTrace("seg-1");
        assertNotNull(row);
        assertEquals("pröxy-å", row.proxy());
    }

    private static TraceMeta meta(String id, String captureId, long startedAt) {
        return new TraceMeta(id, captureId, PLAYER, "abc1234", "proxy-a", 776, "run", startedAt, null, 1);
    }

    /// `HCTR`, the container's version, and whatever stands in for a zstd body.
    private static byte[] trace(int formatVersion, String body) {
        var bytes = body.getBytes(StandardCharsets.UTF_8);
        return ByteBuffer.allocate(6 + bytes.length)
            .put(new byte[]{'H', 'C', 'T', 'R'})
            .putShort((short) formatVersion)
            .put(bytes)
            .array();
    }
}
