package net.hollowcube.proxy.anticheat;

import net.hollowcube.anticheat.log.FrameSource;
import net.hollowcube.anticheat.log.TraceFormat;
import net.hollowcube.anticheat.log.TraceHeader;
import net.hollowcube.anticheat.log.TraceWorld;
import net.hollowcube.anticheat.log.TraceWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Shipping a trace to a store that is really there — the generated ipc server over a directory —
/// so what is sent, what is deleted and what is kept are all the answers a real socket gives.
class TraceShipperTest {

    private static final Logger logger = LoggerFactory.getLogger(TraceShipperTest.class);
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    private static final Duration BOUND = Duration.ofSeconds(10);
    private static final Instant STARTED = Instant.parse("2026-08-29T12:00:00Z");
    private static final Instant ENDED = Instant.parse("2026-08-29T12:01:00Z");

    @TempDir
    Path directory;
    /// Outside the spool on purpose: the sweeper's cap weighs the whole spool volume, and a stored
    /// blob is not the proxy's problem any more.
    @TempDir
    Path storeDirectory;

    private StubStore store;

    /// The metrics are process-wide statics, so each test starts them from zero.
    @BeforeEach
    void setUp() {
        AnticheatMetrics.traces.clear();
        AnticheatMetrics.dropped.clear();
        store = StubStore.start(storeDirectory, 0);
    }

    @AfterEach
    void tearDown() {
        store.close();
    }

    @Test
    void testAStoredTraceIsDeletedAndReported() {
        var shipper = shipper(1 << 20);
        var trace = trace("seg-1", "run-1");

        shipper.ship(trace, null);

        await(() -> !Files.exists(trace));
        assertEquals(1, store.puts().size());
        var put = store.puts().getFirst();
        var meta = put.meta();
        assertEquals("seg-1", meta.id());
        assertEquals("run-1", meta.captureId());
        assertEquals(PLAYER.toString(), meta.playerId());
        assertEquals("run", meta.reason());
        assertEquals(776, meta.clientPvn());
        assertEquals(TraceFormat.VERSION_LATEST, meta.formatVersion());
        assertEquals(STARTED.toEpochMilli(), meta.startedAt());
        assertEquals(ENDED.toEpochMilli(), meta.endedAt());
        assertTrue(Files.exists(storeDirectory.resolve("seg-1.trace")));

        assertEquals(1, AnticheatMetrics.traces.labels("RUN", "STOP", "shipped").get());
        shipper.close(BOUND);
    }

    /// A refusal is final: the trace goes somewhere a sweep will not pick it up again, and the
    /// backend is told nothing, because nothing was stored.
    @Test
    void testARefusedTraceIsPutAsideRatherThanRetried() {
        var shipper = shipper(1 << 20);
        var trace = trace("seg-2", "run-2");
        store.answer(400);

        shipper.ship(trace, null);

        await(() -> Files.exists(directory.resolve("failed").resolve("seg-2.trace")));
        assertFalse(Files.exists(trace));
        assertEquals(1, store.puts().size());
        assertEquals(1, AnticheatMetrics.traces.labels("RUN", "STOP", "refused").get());
        shipper.close(BOUND);
    }

    @Test
    void testAStoreThatWasDownIsRetried() {
        var shipper = shipper(1 << 20);
        var trace = trace("seg-3", "run-3");
        store.answer(503);

        shipper.ship(trace, null);

        await(() -> !Files.exists(trace));
        assertEquals(2, store.puts().size());
        assertEquals(List.of("seg-3", "seg-3"), ids(store.puts()));
        shipper.close(BOUND);
    }

    /// A header naming no player is one the store has no row to file, which is worked out here
    /// rather than by shipping half a megabyte to be told the same thing.
    @Test
    void testATraceWithNoPlayerIsPutAsideWithoutAShip() {
        var shipper = shipper(1 << 20);
        var trace = trace("seg-7", "run-7", null);

        shipper.ship(trace, null);

        await(() -> Files.exists(directory.resolve("failed").resolve("seg-7.trace")));
        assertTrue(store.puts().isEmpty());
        assertEquals(1, AnticheatMetrics.traces.labels("RUN", "STOP", "refused").get());
        shipper.close(BOUND);
    }

    /// Everything the sweeper is for: traces a previous process left behind, and a spool that has
    /// outgrown its cap.
    @Test
    void testTheSweeperShipsLeftoversAndEnforcesTheCap() throws IOException {
        var oldest = trace("seg-old", "run-4");
        var newest = trace("seg-new", "run-5");
        Files.setLastModifiedTime(oldest, FileTime.fromMillis(1_000));
        Files.setLastModifiedTime(newest, FileTime.fromMillis(2_000));
        // Room for one of the two, so the oldest is the one that goes.
        var shipper = shipper(Files.size(newest) + 1);

        shipper.start();

        await(() -> !Files.exists(oldest) && !Files.exists(newest));
        assertEquals(List.of("seg-new"), ids(store.puts()));
        assertEquals(1, AnticheatMetrics.dropped.labels("spool_cap").get());
        shipper.close(BOUND);
    }

    /// A shutdown does not wait for a store that is not answering: the grace runs out, the trace
    /// stays on the spool volume and the next process sweeps it up.
    @Test
    void testCloseGivesUpOnASlowStoreWithinTheGrace() {
        var shipper = shipper(1 << 20);
        var trace = trace("seg-6", "run-6");
        store.delay(Duration.ofSeconds(2));

        shipper.ship(trace, null);
        await(() -> !store.puts().isEmpty());

        var started = System.nanoTime();
        shipper.close(Duration.ofMillis(200));
        var elapsed = Duration.ofNanos(System.nanoTime() - started);

        assertTrue(elapsed.toMillis() < 1_500, "close took " + elapsed);
        assertTrue(Files.exists(trace));
    }

    @Test
    void testATraceIdIsTheFileNameTheStoreWouldTake() {
        assertEquals("seg-1", TraceShipper.traceId(Path.of("/spool/seg-1.trace")));
        assertNull(TraceShipper.traceId(Path.of("/spool/.hidden.trace")));
        assertNull(TraceShipper.traceId(Path.of("/spool/not a trace.trace")));
    }

    private TraceShipper shipper(long spoolMaxBytes) {
        var config = new AnticheatConfig(true, directory, Duration.ofSeconds(60), 1 << 20,
            spoolMaxBytes, Duration.ZERO, Duration.ofSeconds(1));
        return new TraceShipper(store.url(), config, Duration.ofSeconds(2), Duration.ofMillis(20),
            Duration.ofMillis(50));
    }

    private Path trace(String id, String captureId) {
        return trace(id, captureId, PLAYER);
    }

    /// A real trace file, since the shipper reads the header of anything it did not get one for.
    private Path trace(String id, String captureId, @Nullable UUID playerId) {
        var traces = directory.resolve("traces");
        try {
            Files.createDirectories(traces);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        var path = traces.resolve(id + ".trace");
        var header = new TraceHeader(TraceFormat.VERSION_LATEST, 776, "vanilla", playerId, "Tester",
            "connection-1", captureId, TraceHeader.Reason.RUN, TraceHeader.ClosedBy.STOP, null,
            null, "proxy-test", "test", STARTED, ENDED, null, TraceHeader.Flags.NONE,
            TraceHeader.Counters.EMPTY, Map.of());
        TraceWriter.assemble(path, header, List.of(), TraceWorld.EMPTY, FrameSource.EMPTY);
        return path;
    }

    private static List<String> ids(List<StubStore.Put> puts) {
        return puts.stream().map(put -> put.meta().id()).toList();
    }

    private static void await(BooleanSupplier done) {
        var deadline = System.nanoTime() + BOUND.toNanos();
        while (System.nanoTime() < deadline) {
            if (done.getAsBoolean()) return;
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw new AssertionError("timed out after " + BOUND);
    }
}
