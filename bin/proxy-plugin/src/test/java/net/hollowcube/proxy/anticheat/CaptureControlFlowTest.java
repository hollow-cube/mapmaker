package net.hollowcube.proxy.anticheat;

import io.netty.channel.embedded.EmbeddedChannel;
import net.hollowcube.anticheat.capture.CaptureClock;
import net.hollowcube.anticheat.capture.TrimPolicy;
import net.hollowcube.anticheat.control.CaptureControl;
import net.hollowcube.anticheat.log.TraceHeader;
import net.hollowcube.anticheat.log.TraceReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The backend driving a capture over the control channel, end to end on a real engine: what a
/// start opens, what a stop closes and ships, and what happens to a message that has no business
/// being on the channel.
class CaptureControlFlowTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    private static final Duration BOUND = Duration.ofSeconds(10);

    @TempDir
    Path directory;
    /// Outside the spool on purpose: the sweeper's cap weighs the whole spool volume, and a stored
    /// blob is not the proxy's problem any more.
    @TempDir
    Path storeDirectory;

    private StubStore store;
    private TraceShipper shipper;
    private AnticheatConnections installer;
    private EmbeddedChannel channel;

    @BeforeEach
    void setUp() {
        AnticheatMetrics.capturesActive.clear();
        store = StubStore.start(storeDirectory, 0);
        var config = new AnticheatConfig(true, directory, Duration.ofSeconds(60), 1 << 20, 1 << 20,
            Duration.ofSeconds(5));
        shipper = new TraceShipper(store.url(), config, Duration.ofSeconds(2), Duration.ofMillis(20),
            Duration.ofMillis(50));
        installer = new AnticheatConnections(config,
            CaptureClock.SYSTEM, "proxy-test", "test", () -> false, shipper);
        channel = TapPipeline.channel(null, true);
    }

    @AfterEach
    void tearDown() {
        shipper.close(BOUND);
        store.close();
        installer.close();
        channel.finishAndReleaseAll();
    }

    @Test
    void testStartOpensACaptureAndStopShipsIt() {
        installer.join(player(), PLAYER, "Tester", 776, () -> null);

        installer.handleBackend(PLAYER, "Tester", new CaptureControl.Start("run-1", TraceHeader.Reason.RUN,
            TraceHeader.Cohort.TRUSTED, TrimPolicy.DEFAULT).encode());
        channel.runPendingTasks();
        assertEquals(1, AnticheatMetrics.capturesActive.get());

        installer.handleBackend(PLAYER, "Tester", new CaptureControl.Stop("run-1").encode());
        channel.runPendingTasks();
        assertEquals(0, AnticheatMetrics.capturesActive.get());

        await(() -> !store.puts().isEmpty());
        var put = store.puts().getFirst();
        assertEquals("run-1", put.meta().captureId());
        assertEquals("run", put.meta().reason());
        var header = stored(put.meta().id());
        assertEquals(TraceHeader.Cohort.TRUSTED, header.cohort());
        assertEquals(TraceHeader.ClosedBy.STOP, header.closedBy());
    }

    /// A stop for a capture the proxy does not have open leaves the one it does have alone.
    @Test
    void testStopForAnotherCaptureIsIgnored() {
        installer.join(player(), PLAYER, "Tester", 776, () -> null);

        installer.handleBackend(PLAYER, "Tester", new CaptureControl.Start("run-1", TraceHeader.Reason.SAMPLE, null,
            TrimPolicy.DEFAULT).encode());
        installer.handleBackend(PLAYER, "Tester", new CaptureControl.Stop("run-2").encode());
        channel.runPendingTasks();

        assertEquals(1, AnticheatMetrics.capturesActive.get());
        assertTrue(store.puts().isEmpty());
    }

    @Test
    void testAnUnreadableMessageIsWarnedRatherThanThrown() {
        installer.join(player(), PLAYER, "Tester", 776, () -> null);

        installer.handleBackend(PLAYER, "Tester", new byte[]{99, '{', '}'});
        installer.handleBackend(PLAYER, "Tester", "not json at all".getBytes(StandardCharsets.UTF_8));
        installer.handleBackend(PLAYER, "Tester", new CaptureControl.Start(null, TraceHeader.Reason.RUN, null,
            TrimPolicy.DEFAULT).encode());
        channel.runPendingTasks();

        assertEquals(0, AnticheatMetrics.capturesActive.get());
        assertTrue(store.puts().isEmpty());
    }

    /// Nothing is tapped for a player who was never installed, and saying so is not an exception.
    @Test
    void testAMessageForAnUntappedPlayerIsDropped() {
        installer.handleBackend(PLAYER, "Tester", new CaptureControl.Stop("run-1").encode());

        assertEquals(0, AnticheatMetrics.capturesActive.get());
        assertTrue(store.puts().isEmpty());
    }

    /// The player leaving is the same close as a shutdown as far as the trace is concerned: the
    /// engine assembles what it has and the shipper still gets it out.
    @Test
    void testATraceFromADisconnectStillShips() {
        installer.join(player(), PLAYER, "Tester", 776, () -> null);

        installer.handleBackend(PLAYER, "Tester", new CaptureControl.Start("run-3", TraceHeader.Reason.FLAG, null,
            TrimPolicy.DEFAULT).encode());
        channel.runPendingTasks();

        // The channel going away is what a disconnect is, and the tap tells the engine before
        // velocity gets round to the event.
        channel.close();
        channel.runPendingTasks();
        installer.quit(PLAYER);

        await(() -> !store.puts().isEmpty());
        assertEquals(TraceHeader.ClosedBy.DISCONNECT, stored(store.puts().getFirst().meta().id()).closedBy());
        assertEquals(0, AnticheatMetrics.capturesActive.get());
        await(() -> isEmpty(directory.resolve("traces")));
    }

    /// Proxy shutdown, from the tap's side: the open capture is closed as `shutdown` and shipped
    /// inside the grace, which is what makes the last traces of a rolling restart complete.
    @Test
    void testAShutdownClosesAndShipsTheOpenCapture() {
        installer.join(player(), PLAYER, "Tester", 776, () -> null);

        installer.handleBackend(PLAYER, "Tester", new CaptureControl.Start("run-4", TraceHeader.Reason.MANUAL, null,
            TrimPolicy.DEFAULT).encode());
        channel.runPendingTasks();

        var started = System.nanoTime();
        installer.close();
        assertTrue(Duration.ofNanos(System.nanoTime() - started).compareTo(BOUND) < 0, "close outran its grace");

        await(() -> !store.puts().isEmpty());
        assertEquals(TraceHeader.ClosedBy.SHUTDOWN, stored(store.puts().getFirst().meta().id()).closedBy());
        assertEquals(0, AnticheatMetrics.capturesActive.get());
    }

    /// The header of the blob the store took, read back off it: the wire carries a `TraceMeta`,
    /// which has no room for the cohort or for how the capture was closed.
    private TraceHeader stored(String id) {
        var path = storeDirectory.resolve(id + ".trace");
        await(() -> Files.isRegularFile(path));
        return TraceReader.header(path);
    }

    private Object player() {
        return new VelocityInternalsTest.FakePlayer(new VelocityInternalsTest.FakeConnection(channel));
    }

    private static boolean isEmpty(Path directory) {
        try (var files = Files.list(directory)) {
            return files.findAny().isEmpty();
        } catch (Exception e) {
            return true;
        }
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
