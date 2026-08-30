package net.hollowcube.proxy.anticheat;

import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import io.prometheus.client.Counter;
import io.netty.util.ResourceLeakDetector;
import net.hollowcube.anticheat.protocol.ByteWriter;
import net.hollowcube.anticheat.protocol.Direction;
import net.hollowcube.anticheat.protocol.Protocol776;
import net.hollowcube.anticheat.protocol.ProtocolState;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static net.hollowcube.proxy.anticheat.TapPipeline.*;
import static org.junit.jupiter.api.Assertions.*;

/// Replays the checked-in real-client 776 captures through the tap in an `EmbeddedChannel`.
///
/// The point of every assertion here is that a tapped connection is indistinguishable from an
/// untapped one apart from the pings: every frame comes out the far side byte for byte, the pongs
/// to our own pings never reach the backend, and the states the tap reads frames in are the ones
/// the client really used - which the fixture recorded from the other end of the same wire.
class AnticheatTapTest {

    private static final Logger logger = LoggerFactory.getLogger(AnticheatTapTest.class);

    private static final int BUNDLE_DELIMITER = Protocol776.packetId(ProtocolState.PLAY, Direction.S2C, "bundle_delimiter");
    private static final int PLAY_PING = Protocol776.packetId(ProtocolState.PLAY, Direction.S2C, "ping");
    private static final int PLAY_PONG = Protocol776.packetId(ProtocolState.PLAY, Direction.C2S, "pong");

    private static ResourceLeakDetector.Level leakLevel;

    @BeforeAll
    static void detectLeaks() {
        leakLevel = ResourceLeakDetector.getLevel();
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);
    }

    @AfterAll
    static void restoreLeakDetection() {
        ResourceLeakDetector.setLevel(leakLevel);
    }

    /// The metrics are process-wide statics, so each test (and each fixture replay, which asserts
    /// absolute counts) starts them from zero.
    @BeforeEach
    void resetMetrics() {
        AnticheatMetrics.frames.clear();
        AnticheatMetrics.bytes.clear();
        AnticheatMetrics.pings.clear();
        AnticheatMetrics.pongsSwallowed.clear();
        AnticheatMetrics.dropped.clear();
    }

    @Test
    void testFixturesReplayThroughAViaPipeline() {
        for (var fixture : fixtures()) replay(fixture, true);
    }

    @Test
    void testFixturesReplayWithoutVia() {
        for (var fixture : fixtures()) replay(fixture, false);
    }

    /// Every frame of one fixture, C2S as inbound reads and S2C as outbound writes flushed on tick
    /// and bundle boundaries, with the client answering each injected ping.
    private void replay(Path file, boolean via) {
        var source = file.getFileName() + (via ? " (via)" : "");
        var fixture = FixtureReader.read(file);
        var frames = fixture.frames();
        assertEquals(Protocol776.PROTOCOL_VERSION, fixture.header().pv(), source);

        resetMetrics();
        var sink = new RecordingSink();
        var clock = new ManualClock();
        // The tap only ever goes in at PostLoginEvent, so the handshake is not its problem; the
        // fixture is a whole session, and the replay picks it up where the tap would have.
        var tap = new AnticheatTap(sink, clock, () -> false, ProtocolState.LOGIN,
            ProtocolState.LOGIN);
        var channel = channel(tap, via);

        var expected = new Counts();
        var pending = new ArrayList<byte[]>();
        boolean inBundle = false;
        boolean pingSet = false;
        int lastPingId = 0;

        for (var frame : frames) {
            if (frame.state() == ProtocolState.HANDSHAKE) continue;
            clock.set(frame.tNs());
            var entry = Protocol776.lookup(frame.state(), frame.direction(), frame.packetId());
            var bytes = frame(frame.packetId(), frame.body());

            if (frame.direction() == Direction.C2S) {
                assertEquals(frame.state(), tap.c2sState(), source + " c2s state before " + entry.name());
                assertPassesThroughInbound(channel, bytes, source + " " + entry.name());
                if (entry.kept()) expected.keep(Direction.C2S, bytes.length);
                continue;
            }

            assertEquals(frame.state(), tap.s2cState(), source + " s2c state before " + entry.name());
            boolean play = frame.state() == ProtocolState.PLAY;
            if (play && entry.pingSet()) pingSet = true;
            if (entry.kept()) expected.keep(Direction.S2C, bytes.length);
            pending.add(bytes);
            channel.write(buffer(bytes));
            if (play && frame.packetId() == BUNDLE_DELIMITER) inBundle = !inBundle;
            if (tap.s2cState() != ProtocolState.PLAY) {
                // Configuration has no ping set, and a bundle cannot survive the phase change.
                pingSet = false;
                inBundle = false;
            }
            if (inBundle) continue;

            boolean expectPing = pingSet && tap.s2cState() == ProtocolState.PLAY;
            channel.flush();
            for (var written : pending) assertArrayEquals(written, bytes(readOutbound(channel, source)));
            pending.clear();

            if (!expectPing) {
                assertNull(channel.readOutbound(), source + " injected a ping with nothing to time");
                continue;
            }
            int pingId = tap.lastPingId();
            assertTrue(pingId > 0, source + " ping id is not a sequence: " + pingId);
            assertTrue(pingId > lastPingId, source + " ping ids did not advance");
            lastPingId = pingId;
            assertArrayEquals(frame(PLAY_PING, new ByteWriter(4).i32(AnticheatTap.PROXY_PING_BIT | pingId).toByteArray()),
                    bytes(readOutbound(channel, source)), source + " injected ping");
            expected.pings++;
            expected.keep(Direction.S2C, frame(PLAY_PING, new byte[4]).length);
            pingSet = false;

            // The client answers, and the backend must never see it: it counts ping ids up from
            // zero and would take ours for one of its own.
            if (tap.c2sState() == ProtocolState.PLAY) {
                var pong = frame(PLAY_PONG, new ByteWriter(4).i32(AnticheatTap.PROXY_PING_BIT | pingId).toByteArray());
                var buf = buffer(pong);
                channel.writeInbound(buf);
                assertNull(channel.readInbound(), source + " forwarded a pong to an injected ping");
                assertEquals(0, buf.refCnt(), source + " leaked the swallowed pong");
                expected.pongs++;
                expected.keep(Direction.C2S, pong.length);
            }
        }

        // A fixture that ends inside a bundle leaves the last frames unflushed.
        if (!pending.isEmpty()) {
            channel.flush();
            for (var written : pending) assertArrayEquals(written, bytes(readOutbound(channel, source)));
            assertNull(channel.readOutbound(), source + " injected a ping inside a bundle");
        }

        assertTrue(expected.pings > 0, source + " injected no pings at all");
        assertTrue(expected.pongs > 0, source + " swallowed no pongs at all");
        assertFalse(tap.failed(), source + " the tap gave up");
        assertEquals(0, tap.exceptions(), source);

        assertEquals((double) expected.frames[0], counter(AnticheatMetrics.frames, "c2s"), source + " c2s frames");
        assertEquals((double) expected.frames[1], counter(AnticheatMetrics.frames, "s2c"), source + " s2c frames");
        assertEquals((double) expected.bytes[0], counter(AnticheatMetrics.bytes, "c2s"), source + " c2s bytes");
        assertEquals((double) expected.bytes[1], counter(AnticheatMetrics.bytes, "s2c"), source + " s2c bytes");
        assertEquals((double) expected.pings, AnticheatMetrics.pings.get(), source + " pings");
        assertEquals((double) expected.pongs, AnticheatMetrics.pongsSwallowed.get(), source + " pongs");
        assertEquals(expected.frames[0] + expected.frames[1], (long) sink.frames.size(), source + " frames kept");
        assertEquals(lastPingId, sink.frames.getLast().pingId(), source + " last frame's ping bracket");

        channel.close();
        assertTrue(sink.disconnected, source + " the sink was not told the connection went away");
        assertNull(channel.readInbound(), source + " left a frame behind");
        assertNull(channel.readOutbound(), source + " left a frame behind");
        assertFalse(channel.finishAndReleaseAll(), source + " left a message in the channel");
    }

    @Test
    void testAPongWithABackendIdPassesThrough() {
        var sink = new RecordingSink();
        var tap = play(sink);
        var channel = channel(tap, false);

        var pong = frame(PLAY_PONG, new ByteWriter(4).i32(17).toByteArray());
        assertPassesThroughInbound(channel, pong, "backend pong");
        assertEquals(1, sink.frames.size());
        assertFalse(channel.finishAndReleaseAll());
    }

    @Test
    void testOnlyAPongForAnOutstandingInjectedPingIsSwallowed() {
        var sink = new RecordingSink();
        var tap = new AnticheatTap(sink, new ManualClock(), () -> false, ProtocolState.PLAY,
            ProtocolState.PLAY);
        var channel = channel(tap, false);

        // Being in the proxy's id space is not enough: a connection replayed from a capture taken
        // behind another tap carries that tap's pongs, which this one never asked for.
        var stranger = frame(PLAY_PONG, new ByteWriter(4).i32(AnticheatTap.PROXY_PING_BIT | 7).toByteArray());
        assertPassesThroughInbound(channel, stranger, "a pong for a ping we never issued");

        channel.write(buffer(frame(Protocol776.packetId(ProtocolState.PLAY, Direction.S2C, "set_health"), new byte[12])));
        channel.flush();
        readOutbound(channel, "health");
        readOutbound(channel, "ping");

        int pingId = tap.lastPingId();
        assertTrue(pingId > 0, "ping id is not a sequence: " + pingId);
        var pong = frame(PLAY_PONG, new ByteWriter(4).i32(AnticheatTap.PROXY_PING_BIT | pingId).toByteArray());
        var buf = buffer(pong);
        channel.writeInbound(buf);
        assertNull(channel.readInbound(), "forwarded the pong to our own ping");
        assertEquals(0, buf.refCnt(), "leaked the swallowed pong");

        // Answered is answered. A second pong for the same id is the client repeating itself, and
        // the tap has no ping of its own left for it to be about.
        assertPassesThroughInbound(channel, pong, "a second pong for an id already answered");

        assertEquals(1d, AnticheatMetrics.pongsSwallowed.get(), "swallowed more than the one pong it asked for");
        assertEquals(3, sink.frames.stream().filter(frame -> frame.direction() == Direction.C2S).count(),
                "a pong that passed through was not recorded");
        assertFalse(channel.finishAndReleaseAll());
    }

    @Test
    void testAFlushWithoutAPingSetFrameInjectsNothing() {
        var tap = play(new RecordingSink());
        var channel = channel(tap, false);

        // keep_alive is kept but is not in the ping set: nothing about it needs timing.
        var keepAlive = frame(Protocol776.packetId(ProtocolState.PLAY, Direction.S2C, "keep_alive"), new byte[8]);
        channel.write(buffer(keepAlive));
        channel.flush();

        assertArrayEquals(keepAlive, bytes(readOutbound(channel, "keep alive")));
        assertNull(channel.readOutbound());
        assertEquals(0, tap.lastPingId());
        assertFalse(channel.finishAndReleaseAll());
    }

    /// The engine answers the fences the table cannot decide on the id alone (own-player knockback
    /// and friends); to the tap that answer reads exactly like a ping-set frame.
    @Test
    void testAFenceTheSinkAsksForInjectsAPing() {
        var sink = new RecordingSink();
        sink.fence = true;
        var tap = play(sink);
        var channel = channel(tap, false);

        var keepAlive = frame(Protocol776.packetId(ProtocolState.PLAY, Direction.S2C, "keep_alive"), new byte[8]);
        channel.write(buffer(keepAlive));
        channel.flush();

        assertArrayEquals(keepAlive, bytes(readOutbound(channel, "keep alive")));
        assertArrayEquals(frame(PLAY_PING,
                new ByteWriter(4).i32(AnticheatTap.PROXY_PING_BIT | tap.lastPingId()).toByteArray()),
            bytes(readOutbound(channel, "ping")));
        assertFalse(channel.finishAndReleaseAll());
    }

    @Test
    void testNoPingIsInjectedInsideABundle() {
        var tap = play(new RecordingSink());
        var channel = channel(tap, false);
        var delimiter = frame(BUNDLE_DELIMITER, new byte[0]);
        var health = frame(Protocol776.packetId(ProtocolState.PLAY, Direction.S2C, "set_health"), new byte[12]);

        channel.write(buffer(delimiter));
        channel.write(buffer(health));
        channel.flush();
        assertArrayEquals(delimiter, bytes(readOutbound(channel, "open")));
        assertArrayEquals(health, bytes(readOutbound(channel, "health")));
        assertNull(channel.readOutbound(), "a ping was injected inside a bundle");

        // The bundle closes and the ping the frame asked for lands right after it.
        channel.write(buffer(delimiter));
        channel.flush();
        assertArrayEquals(delimiter, bytes(readOutbound(channel, "close")));
        assertArrayEquals(frame(PLAY_PING,
                new ByteWriter(4).i32(AnticheatTap.PROXY_PING_BIT | tap.lastPingId()).toByteArray()),
                bytes(readOutbound(channel, "ping")));
        assertFalse(channel.finishAndReleaseAll());
    }

    /// Velocity disconnects everybody before ProxyShutdownEvent, so the only difference between a
    /// player leaving and the proxy going down is what the tap asks at channelInactive.
    @Test
    void testAChannelClosedUnderAShutdownTellsTheSinkSo() {
        var leaving = new RecordingSink();
        channel(play(leaving, false), false).close();
        assertTrue(leaving.disconnected, "the sink was not told the connection went away");
        assertFalse(leaving.shutdown, "a player leaving is not a shutdown");

        var goingDown = new RecordingSink();
        channel(play(goingDown, true), false).close();
        assertTrue(goingDown.disconnected, "the sink was not told the connection went away");
        assertTrue(goingDown.shutdown, "a channel closed under a shutdown reads as a player leaving");
    }

    @Test
    void testAnUnknownIdPassesThroughAndIsNotKept() {
        var sink = new RecordingSink();
        var tap = play(sink);
        var channel = channel(tap, false);

        var unknown = frame(0x7F, new byte[]{1, 2, 3});
        assertPassesThroughInbound(channel, unknown, "unknown id");
        assertTrue(sink.frames.isEmpty(), "an id we cannot name was recorded");
        assertFalse(channel.finishAndReleaseAll());
    }

    /// The tap only ever goes in from login onwards, so handshake is a state it has no table for.
    /// A frame it cannot name is a frame it cannot replay: it goes through untouched and is counted.
    @Test
    void testAFrameInAStateTheTapDoesNotTrackIsCountedAndNotKept() {
        var sink = new RecordingSink();
        var tap = new AnticheatTap(sink, new ManualClock(), () -> false,
            ProtocolState.HANDSHAKE, ProtocolState.HANDSHAKE);
        var channel = channel(tap, false);

        var intention = frame(0, new byte[]{1, 2, 3});
        assertPassesThroughInbound(channel, intention, "a frame in handshake");

        channel.write(buffer(intention));
        channel.flush();
        assertArrayEquals(intention, bytes(readOutbound(channel, "a frame in handshake")));
        assertNull(channel.readOutbound(), "a ping was injected outside play");

        assertTrue(sink.frames.isEmpty(), "a frame in a state with no table was recorded");
        assertEquals(2, AnticheatMetrics.dropped.labels("unknown_state").get());
        assertEquals(0d, counter(AnticheatMetrics.frames, "c2s"));
        assertEquals(0d, counter(AnticheatMetrics.frames, "s2c"));
        assertFalse(tap.failed());
        assertFalse(channel.finishAndReleaseAll());
    }

    private AnticheatTap play(RecordingSink sink) {
        return play(sink, false);
    }

    private AnticheatTap play(RecordingSink sink, boolean shuttingDown) {
        return new AnticheatTap(sink, new ManualClock(),
            () -> shuttingDown, ProtocolState.PLAY, ProtocolState.PLAY);
    }

    private static void assertPassesThroughInbound(EmbeddedChannel channel, byte[] bytes,
                                                   String what) {
        channel.writeInbound(buffer(bytes));
        ByteBuf read = channel.readInbound();
        assertNotNull(read, what + " did not pass through");
        assertArrayEquals(bytes, bytes(read), what + " was altered");
        read.release();
    }

    private static ByteBuf readOutbound(EmbeddedChannel channel, String what) {
        ByteBuf written = channel.readOutbound();
        assertNotNull(written, what + " was not written");
        return written;
    }

    private static double counter(Counter counter, String dir) {
        return counter.labels(dir).get();
    }

    /// What the tap should have counted, indexed by [Direction#ordinal()].
    private static final class Counts {

        private final long[] frames = new long[2];
        private final long[] bytes = new long[2];
        private long pings;
        private long pongs;

        void keep(Direction direction, int length) {
            frames[direction.ordinal()]++;
            bytes[direction.ordinal()] += length;
        }
    }

    private static List<Path> fixtures() {
        var directory = AnticheatTapTest.class.getResource("/fixtures/" + Protocol776.PROTOCOL_VERSION);
        Assumptions.assumeTrue(directory != null, "no 776 capture fixtures present");
        try (Stream<Path> files = Files.list(Path.of(directory.toURI()))) {
            var result = new ArrayList<>(files.filter(file -> file.toString().endsWith(".hcpt.zst")).toList());
            result.sort(Path::compareTo);
            Assumptions.assumeFalse(result.isEmpty(), "no 776 capture fixtures present");
            return List.copyOf(result);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
