package net.hollowcube.anticheat.capture;

import net.hollowcube.anticheat.log.Trace;
import net.hollowcube.anticheat.log.TraceHeader;
import net.hollowcube.anticheat.log.TraceReader;
import net.hollowcube.anticheat.log.Frame;
import net.hollowcube.anticheat.protocol.*;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static net.hollowcube.anticheat.capture.TestCapture.*;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;

/// The engine end to end, on frames the test builds itself: the state machine, what reaches the
/// spool and the file, and what happens when the writer cannot keep up.
class CaptureEngineTest {

    @TempDir
    Path directory;

    @Test
    void testACaptureWritesATraceTheReaderReadsBack() throws Exception {
        var clock = new TestCapture.ManualClock();
        var traces = new TestCapture.Traces();
        var engine = engine(clock, traces);

        join(engine, clock, 0);
        engine.start("run-1", TraceHeader.Reason.RUN, TraceHeader.Cohort.TRUSTED, TrimPolicy.EVERYTHING);
        assertEquals(CaptureEngine.Status.CAPTURING, engine.status());
        assertEquals("run-1", engine.captureId());

        for (int second = 1; second <= 10; second++) {
            clock.set(second * SECOND);
            move(engine, second * SECOND, second, 64, 0);
        }
        clock.set(11 * SECOND);
        engine.stop(TraceHeader.ClosedBy.STOP);
        assertEquals(CaptureEngine.Status.IDLE, engine.status());
        assertNull(engine.captureId());

        var written = traces.take();
        var header = written.header();
        assertEquals("run-1", header.captureId());
        assertEquals(TraceHeader.Reason.RUN, header.reason());
        assertEquals(TraceHeader.ClosedBy.STOP, header.closedBy());
        assertEquals(TraceHeader.Cohort.TRUSTED, header.cohort());
        assertEquals(new TraceHeader.Trim(-1, 8), header.trim());
        assertEquals("connection-1", header.connectionId());
        assertEquals(10, header.counters().frames());
        assertEquals(0, header.counters().droppedFrames());
        assertEquals(TraceHeader.Flags.NONE, header.flags());
        assertEquals(Duration.ofSeconds(11), Duration.between(header.startedAt(), header.endedAt()));

        var trace = TraceReader.read(written.path());
        assertFalse(trace.truncated());
        assertEquals(10, trace.frames().size());
        // Frame times are relative to the snapshot the trace starts at.
        assertEquals(SECOND, trace.frames().getFirst().tNs());
        assertEquals(10 * SECOND, trace.frames().getLast().tNs());
        assertEquals(header.counters().chunks(), trace.chunks().size());
        assertEquals(header.counters().preludeFrames(), trace.prelude().size());
        assertSpoolEmpty();

        engine.close();
    }

    @Test
    void testTheStartSnapshotCarriesTheWorldAndTheTrimApplies() throws Exception {
        var clock = new TestCapture.ManualClock();
        var traces = new TestCapture.Traces();
        var engine = engine(clock, traces);

        join(engine, clock, 0);
        for (int x = -4; x <= 4; x++)
            for (int z = -4; z <= 4; z++)
                feed(engine, SECOND, ProtocolState.PLAY, Direction.S2C, "level_chunk_with_light", chunk(x, z));

        clock.set(2 * SECOND);
        engine.start("run-2", TraceHeader.Reason.RUN, null, new TrimPolicy(1, 8));
        move(engine, 3 * SECOND, 0.5, 64, 0.5);
        clock.set(4 * SECOND);
        engine.stop(TraceHeader.ClosedBy.STOP);

        var header = traces.take().header();
        // The player only ever stood in 0,0, so a radius of one is the nine chunks around it.
        assertEquals(9, header.counters().chunks());
        assertEquals(new TraceHeader.Trim(1, 8), header.trim());
        assertTrue(header.counters().preludeFrames() > 0, "the login and the player position are the least of it");

        engine.close();
    }

    @Test
    void testChunkFramesAreStoredAsTheirBlockSectionsAlone() throws Exception {
        var clock = new TestCapture.ManualClock();
        var traces = new TestCapture.Traces();
        var engine = engine(clock, traces);

        join(engine, clock, 0);
        engine.start("run-light", TraceHeader.Reason.RUN, null, TrimPolicy.EVERYTHING);
        var sent = chunk(0, 0);
        feed(engine, SECOND, ProtocolState.PLAY, Direction.S2C, "level_chunk_with_light", sent);
        clock.set(2 * SECOND);
        engine.stop(TraceHeader.ClosedBy.STOP);

        var trace = TraceReader.read(traces.take().path());
        var stored = trace.frames().stream()
            .filter(frame -> frame.packetId() == Protocol776.packetId(ProtocolState.PLAY, Direction.S2C,
                "level_chunk_with_light"))
            .findFirst()
            .orElseThrow();

        assertTrue(stored.bytes().length < sent.toByteArray().length);

        var read = S2CLevelChunkWithLight.V776.decode(new ByteReader(stored.bytes()));
        assertEquals(0, read.blockEntitiesAndLight().length());
        // A varint zero, which is an empty Heightmap.Types -> long[] map.
        assertArrayEquals(new byte[]{0}, read.heightmaps().toByteArray());
        assertEquals(sent.chunkX(), read.chunkX());
        assertEquals(sent.chunkZ(), read.chunkZ());
        // The sections are the whole point of keeping the frame, so they survive intact. Section
        // holds arrays, so they compare as the bytes they were read out of.
        assertArrayEquals(sections(sent), sections(read));

        engine.close();
    }

    @Test
    void testASecondStartSupersedesTheFirst() throws Exception {
        var clock = new TestCapture.ManualClock();
        var traces = new TestCapture.Traces();
        var engine = engine(clock, traces);

        join(engine, clock, 0);
        engine.start("run-a", TraceHeader.Reason.RUN, null, TrimPolicy.EVERYTHING);
        move(engine, SECOND, 1, 64, 0);
        clock.set(2 * SECOND);
        engine.start("run-b", TraceHeader.Reason.SAMPLE, TraceHeader.Cohort.RANDOM, TrimPolicy.EVERYTHING);
        assertEquals("run-b", engine.captureId());
        move(engine, 3 * SECOND, 2, 64, 0);
        clock.set(4 * SECOND);
        engine.stop(TraceHeader.ClosedBy.STOP);

        var first = traces.take().header();
        assertEquals("run-a", first.captureId());
        assertEquals(TraceHeader.ClosedBy.SUPERSEDED, first.closedBy());
        assertEquals(1, first.counters().frames());

        var second = traces.take().header();
        assertEquals("run-b", second.captureId());
        assertEquals(TraceHeader.ClosedBy.STOP, second.closedBy());
        assertEquals(TraceHeader.Cohort.RANDOM, second.cohort());
        assertEquals(1, second.counters().frames());

        engine.close();
    }

    @Test
    void testStoppingWhileIdleDoesNothing() throws Exception {
        var clock = new TestCapture.ManualClock();
        var traces = new TestCapture.Traces();
        var engine = engine(clock, traces);

        engine.stop(TraceHeader.ClosedBy.STOP);
        engine.disconnect(false);
        assertEquals(CaptureEngine.Status.IDLE, engine.status());
        traces.assertNone();

        engine.close();
        traces.assertNone();
    }

    @Test
    void testFlushingBeforeTheFirstFrameDoesNothing() throws Exception {
        var clock = new TestCapture.ManualClock();
        var traces = new TestCapture.Traces();
        var engine = engine(clock, traces);

        engine.flush("staff-1", TraceHeader.Reason.MANUAL);
        traces.assertNone();

        engine.close();
    }

    @Test
    void testAFlushWhileIdleShipsTheRingOnItsOwn() throws Exception {
        var clock = new TestCapture.ManualClock();
        var traces = new TestCapture.Traces();
        var engine = engine(clock, traces);

        join(engine, clock, 0);
        for (int second = 1; second <= 10; second++) {
            clock.set(second * SECOND);
            move(engine, second * SECOND, second, 64, 0);
        }
        engine.flush(null, TraceHeader.Reason.FLAG);

        var header = traces.take().header();
        assertEquals(CaptureEngine.Status.IDLE, engine.status());
        assertNull(header.captureId());
        assertEquals(TraceHeader.ClosedBy.FLUSH, header.closedBy());
        // The ring has one snapshot, taken at the first frame, so it carries the lot.
        assertEquals(12, header.counters().frames());
        assertEquals(Duration.ofSeconds(10), Duration.between(header.startedAt(), header.endedAt()));

        engine.close();
    }

    @Test
    void testAFlushAfterNinetySecondsUsesTheOlderRingSnapshot() throws Exception {
        var clock = new TestCapture.ManualClock();
        var traces = new TestCapture.Traces();
        var engine = engine(clock, traces);

        join(engine, clock, 0);
        engine.start("run-3", TraceHeader.Reason.RUN, null, TrimPolicy.EVERYTHING);
        for (int second = 1; second <= 95; second++) {
            clock.set(second * SECOND);
            move(engine, second * SECOND, second, 64, 0);
        }

        engine.flush("staff-2", TraceHeader.Reason.MANUAL);
        assertEquals(CaptureEngine.Status.CAPTURING, engine.status(), "a flush leaves the capture alone");

        var written = traces.take();
        var header = written.header();
        assertEquals("staff-2", header.captureId());
        assertEquals(TraceHeader.ClosedBy.FLUSH, header.closedBy());
        assertEquals(TraceHeader.Reason.MANUAL, header.reason());
        // Snapshots land at 0, 30, 60 and 90 seconds; the flush starts from 30, not 90.
        assertEquals(Duration.ofSeconds(65), Duration.between(header.startedAt(), header.endedAt()));
        assertEquals(66, header.counters().frames());

        var trace = TraceReader.read(written.path());
        assertEquals(0, trace.frames().getFirst().tNs(), "the flush starts at its snapshot");
        assertEquals(65 * SECOND, trace.frames().getLast().tNs());

        // And the capture it did not disturb still runs from its own start.
        clock.set(96 * SECOND);
        engine.stop(TraceHeader.ClosedBy.STOP);
        var capture = traces.take().header();
        assertEquals("run-3", capture.captureId());
        assertEquals(95, capture.counters().frames());

        engine.close();
    }

    @Test
    void testTheTimeCapStopsTheCaptureAndMarksIt() throws Exception {
        var clock = new TestCapture.ManualClock();
        var traces = new TestCapture.Traces();
        CaptureEngineConfig config = new CaptureEngineConfig(directory.resolve("spool"),
            directory.resolve("out"), 60 * SECOND, 30 * SECOND, 1 << 20, 1 << 20, 5 * SECOND, 0, 1024,
            TrimPolicy.DEFAULT, Duration.ofSeconds(5));
        var engine = new CaptureEngine(config, TestCapture.identity(), () -> null, clock, traces);

        join(engine, clock, 0);
        engine.start("run-4", TraceHeader.Reason.SAMPLE, null, TrimPolicy.EVERYTHING);
        for (int second = 1; second <= 8; second++) {
            clock.set(second * SECOND);
            move(engine, second * SECOND, second, 64, 0);
        }

        var header = traces.take().header();
        assertEquals(TraceHeader.ClosedBy.STOP, header.closedBy());
        assertTrue(header.flags().spoolTruncated(), "a capture cut short by the time cap is truncated");
        assertEquals(CaptureEngine.Status.IDLE, engine.status());
        assertEquals(5, header.counters().frames());

        engine.close();
    }

    @Test
    void testACaptureBelowTheFloorIsDiscarded() throws Exception {
        var clock = new TestCapture.ManualClock();
        var traces = new TestCapture.Traces();
        CaptureEngineConfig config = new CaptureEngineConfig(directory.resolve("spool"),
            directory.resolve("out"), 60 * SECOND, 30 * SECOND, 1 << 20, 1 << 20, 600 * SECOND,
            3 * SECOND, 1024, TrimPolicy.DEFAULT, Duration.ofSeconds(5));
        var engine = new CaptureEngine(config, TestCapture.identity(), () -> null, clock, traces);

        join(engine, clock, 0);
        engine.start("run-short", TraceHeader.Reason.RUN, null, TrimPolicy.DEFAULT);
        clock.set(SECOND);
        move(engine, SECOND, 1, 64, 0);
        engine.stop(TraceHeader.ClosedBy.STOP);

        assertEquals(CaptureEngine.Status.IDLE, engine.status());
        assertEquals(1, engine.discardedCaptures());

        // The next capture still works, and is the only one that reaches the writer.
        engine.start("run-long", TraceHeader.Reason.RUN, null, TrimPolicy.DEFAULT);
        for (int second = 2; second <= 8; second++) {
            clock.set(second * SECOND);
            move(engine, second * SECOND, second, 64, 0);
        }
        engine.stop(TraceHeader.ClosedBy.STOP);

        var header = traces.take().header();
        assertEquals("run-long", header.captureId());
        assertEquals(1, engine.discardedCaptures());

        // close() waits for the writer, so by here every spool either assembled or was deleted.
        engine.close();
        try (var spools = Files.list(directory.resolve("spool"))) {
            assertEquals(List.of(), spools.toList(), "a spool was left behind");
        }
    }

    @Test
    void testTheRingCapTruncatesAFlushAndSaysSo() throws Exception {
        var clock = new TestCapture.ManualClock();
        var traces = new TestCapture.Traces();
        // Four kilobytes of ring, which a hundred moves is well past.
        var config = new CaptureEngineConfig(directory.resolve("spool"), directory.resolve("out"),
            60 * SECOND, 30 * SECOND, 4096, 1 << 20, 600 * SECOND, 0, 1 << 20, TrimPolicy.DEFAULT,
            Duration.ofSeconds(5));
        var engine = new CaptureEngine(config, TestCapture.identity(), () -> null, clock, traces);

        join(engine, clock, 0);
        for (int frame = 1; frame <= 100; frame++) {
            clock.set(frame * SECOND / 10);
            move(engine, frame * SECOND / 10, frame, 64, 0);
        }
        assertTrue(engine.ring().evictedFrames() > 0, "the cap dropped nothing");
        engine.flush("staff-3", TraceHeader.Reason.MANUAL);

        var written = traces.take();
        var header = written.header();
        assertEquals(TraceHeader.ClosedBy.FLUSH, header.closedBy());
        assertTrue(header.flags().ringTruncated(), "a ring that lost frames is truncated");
        assertFalse(header.flags().spoolTruncated());
        assertTrue(header.counters().frames() < 100, "the whole ring survived a cap it should not have");

        var trace = TraceReader.read(written.path());
        assertFalse(trace.truncated(), "the file itself is whole, it is the ring that lost frames");
        assertEquals(header.counters().frames(), trace.frames().size());

        engine.close();
    }

    @Test
    void testTheSpoolCapTruncatesACaptureAndItStillShips() throws Exception {
        var clock = new TestCapture.ManualClock();
        var traces = new TestCapture.Traces();
        // Half a kilobyte of spool: twenty moves of twenty-five bytes and then no more.
        var config = new CaptureEngineConfig(directory.resolve("spool"), directory.resolve("out"),
            60 * SECOND, 30 * SECOND, 1 << 20, 512, 600 * SECOND, 0, 1 << 20, TrimPolicy.DEFAULT,
            Duration.ofSeconds(5));
        var engine = new CaptureEngine(config, TestCapture.identity(), () -> null, clock, traces);

        join(engine, clock, 0);
        engine.start("run-9", TraceHeader.Reason.RUN, null, TrimPolicy.EVERYTHING);
        int tickEnd = Protocol776.packetId(ProtocolState.PLAY, Direction.C2S, "client_tick_end");
        for (int frame = 1; frame <= 100; frame++) {
            clock.set(frame * SECOND / 10);
            move(engine, frame * SECOND / 10, frame, 64, 0);
            engine.frame(frame * SECOND / 10 + 1, Direction.C2S, ProtocolState.PLAY, tickEnd, Frame.NO_PING, new byte[0]);
        }
        clock.set(11 * SECOND);
        engine.stop(TraceHeader.ClosedBy.STOP);

        var written = traces.take();
        var header = written.header();
        assertEquals(TraceHeader.ClosedBy.STOP, header.closedBy());
        assertTrue(header.flags().spoolTruncated(), "a capture cut short by the spool cap is truncated");
        assertFalse(header.flags().ringTruncated());
        long frames = header.counters().frames();
        assertTrue(frames > 0 && frames < 200, "the spool cap kept " + frames + " of 200 frames");

        var trace = TraceReader.read(written.path());
        assertFalse(trace.truncated(), "the trace stops where the spool stopped, cleanly");
        assertEquals(frames, trace.frames().size());
        assertEquals(tickEnd, trace.frames().getLast().packetId(),
            "the spool stops at the first frame over the cap; the zero-byte tick ends after it do not still fit");
        assertEquals(trace.frames().size() / 2, trace.frames().stream().filter(f -> f.packetId() == tickEnd).count());
        assertSpoolEmpty();

        engine.close();
    }

    @Test
    void testFramesTheWriterCannotKeepUpWithAreDroppedAndCounted() throws Exception {
        var clock = new TestCapture.ManualClock();
        var traces = new TestCapture.Traces();
        var writer = new TestCapture.Deferred();
        CaptureEngineConfig config = new CaptureEngineConfig(directory.resolve("spool"),
            directory.resolve("out"), 60 * SECOND, 30 * SECOND, 1 << 20, 1 << 20, 600 * SECOND, 0, 2,
            TrimPolicy.DEFAULT, Duration.ofSeconds(5));
        var engine = new CaptureEngine(config, TestCapture.identity(), () -> null, clock, traces, writer);

        engine.start("run-5", TraceHeader.Reason.FLAG, null, TrimPolicy.EVERYTHING);
        for (int second = 1; second <= 10; second++) {
            clock.set(second * SECOND);
            move(engine, second * SECOND, second, 64, 0);
        }
        clock.set(11 * SECOND);
        engine.stop(TraceHeader.ClosedBy.STOP);

        // Nothing has run yet, so the queue held exactly two frames and the other eight are gone.
        writer.release();
        var header = traces.take().header();
        assertEquals(2, header.counters().frames());
        assertEquals(8, header.counters().droppedFrames());

        engine.close();
    }

    @Test
    void testDisconnectClosesTheCaptureAndAShutdownDisconnectSaysSo() throws Exception {
        var clock = new TestCapture.ManualClock();
        var traces = new TestCapture.Traces();
        var engine = engine(clock, traces);

        join(engine, clock, 0);
        engine.start("run-6", TraceHeader.Reason.RUN, null, TrimPolicy.EVERYTHING);
        move(engine, SECOND, 1, 64, 0);
        clock.set(2 * SECOND);
        engine.disconnect(false);
        var header = traces.take().header();
        assertEquals(TraceHeader.ClosedBy.DISCONNECT, header.closedBy());
        assertTrue(header.flags().tailUnfenced(), "nothing will ever bound this trace's tail");

        // A proxy shutdown reaches the engine as a disconnect too, because velocity kicks everybody
        // before it fires ProxyShutdownEvent; the tap is what knows which one it was.
        engine.start("run-7", TraceHeader.Reason.RUN, null, TrimPolicy.EVERYTHING);
        move(engine, 3 * SECOND, 2, 64, 0);
        clock.set(4 * SECOND);
        engine.disconnect(true);
        var shutdown = traces.take().header();
        assertEquals(TraceHeader.ClosedBy.SHUTDOWN, shutdown.closedBy());
        assertTrue(shutdown.flags().tailUnfenced());

        engine.start("run-8", TraceHeader.Reason.RUN, null, TrimPolicy.EVERYTHING);
        move(engine, 5 * SECOND, 3, 64, 0);
        engine.close();
        assertEquals(TraceHeader.ClosedBy.SHUTDOWN, traces.take().header().closedBy());
        assertSpoolEmpty();
    }

    /// The conditional fences are answered here rather than in the tap, because this is the side
    /// that decodes: the engine knows the local player's id from the login it applied, so a
    /// knockback aimed at them asks for a ping and everyone else's stays per-entity noise.
    @Test
    void testConditionalFencesFireForTheLocalPlayerOnly() throws Exception {
        var clock = new TestCapture.ManualClock();
        var traces = new TestCapture.Traces();
        var engine = engine(clock, traces);

        join(engine, clock, 0);
        assertTrue(feed(engine, SECOND, ProtocolState.PLAY, Direction.S2C, "set_entity_motion",
            new S2CSetEntityMotion.V776(TestCapture.PLAYER_ID, LpVec3.ZERO)), "knockback on the player");
        assertFalse(feed(engine, SECOND, ProtocolState.PLAY, Direction.S2C, "set_entity_motion",
            new S2CSetEntityMotion.V776(99, LpVec3.ZERO)), "someone else's knockback");
        assertTrue(feed(engine, SECOND, ProtocolState.PLAY, Direction.S2C, "entity_event",
            new S2CEntityEvent.V776(TestCapture.PLAYER_ID, S2CEntityEvent.SWAP_HANDS)), "the player's hand swap");
        assertFalse(feed(engine, SECOND, ProtocolState.PLAY, Direction.S2C, "entity_event",
            new S2CEntityEvent.V776(TestCapture.PLAYER_ID, (byte) 2)), "a cosmetic event on the player");
        assertTrue(feed(engine, SECOND, ProtocolState.PLAY, Direction.S2C, "animate",
            new S2CAnimate.V776(99, S2CAnimate.WAKE_UP)), "anyone waking up writes the bed block");
        assertFalse(feed(engine, SECOND, ProtocolState.PLAY, Direction.S2C, "animate",
            new S2CAnimate.V776(99, 0)), "a swing");

        engine.close();
    }

    @Test
    void testTheHeaderNamesTheChannelsTheClientRegisteredAndTheProxyKnew() throws Exception {
        var clock = new TestCapture.ManualClock();
        var traces = new TestCapture.Traces();
        var engine = new CaptureEngine(TestCapture.config(directory), TestCapture.identity(), () -> "fabric",
            () -> List.of("minecraft:brand", "fabric:registry/sync/direct"), clock, traces);

        join(engine, clock, 0);
        engine.start("run-12", TraceHeader.Reason.RUN, null, TrimPolicy.EVERYTHING);
        feed(engine, SECOND, ProtocolState.PLAY, Direction.C2S, "custom_payload",
            new C2SCustomPayload.V776(CustomPayload.REGISTER_CHANNEL, "noxesium-v3:client_settings".getBytes(StandardCharsets.UTF_8)));
        move(engine, SECOND, 1, 64, 0);
        clock.set(2 * SECOND);
        engine.stop(TraceHeader.ClosedBy.STOP);
        assertEquals("fabric:registry/sync/direct,minecraft:brand,noxesium-v3:client_settings",
            traces.take().header().extras().get(CaptureEngine.CHANNELS_EXTRA));

        engine.close();
    }

    /// The tap goes in at `PostLoginEvent`, which velocity fires off the event loop, so the
    /// client's `minecraft:brand` payload has almost always passed before there is anything on the
    /// pipeline to see it: without the proxy's own answer the brand would be null on every trace.
    @Test
    void testTheHeaderBrandFallsBackToTheProxyUntilTheClientSendsOne() throws Exception {
        var clock = new TestCapture.ManualClock();
        var traces = new TestCapture.Traces();
        var engine = new CaptureEngine(TestCapture.config(directory), TestCapture.identity(),
            () -> "fabric", clock, traces);

        join(engine, clock, 0);
        engine.start("run-10", TraceHeader.Reason.RUN, null, TrimPolicy.EVERYTHING);
        move(engine, SECOND, 1, 64, 0);
        clock.set(2 * SECOND);
        engine.stop(TraceHeader.ClosedBy.STOP);
        assertEquals("fabric", traces.take().header().brand());

        // And the payload wins as soon as the connection carries one of its own.
        engine.start("run-11", TraceHeader.Reason.RUN, null, TrimPolicy.EVERYTHING);
        feed(engine, 3 * SECOND, ProtocolState.PLAY, Direction.C2S, "custom_payload",
            new C2SCustomPayload.V776(CustomPayload.BRAND_CHANNEL, new ByteWriter().utf("vanilla").toByteArray()));
        clock.set(4 * SECOND);
        engine.stop(TraceHeader.ClosedBy.STOP);
        assertEquals("vanilla", traces.take().header().brand());

        engine.close();
    }

    @Test
    void testFramesForDisplayEntitiesAreNotStored() throws Exception {
        var clock = new TestCapture.ManualClock();
        var traces = new TestCapture.Traces();
        var engine = engine(clock, traces);

        join(engine, clock, 0);
        engine.start("run-display", TraceHeader.Reason.RUN, null, TrimPolicy.EVERYTHING);

        // One text display and one pig, then a position sync each.
        feed(engine, SECOND, ProtocolState.PLAY, Direction.S2C, "add_entity", addEntity(900, 132));
        feed(engine, SECOND, ProtocolState.PLAY, Direction.S2C, "add_entity", addEntity(901, 69));
        feed(engine, 2 * SECOND, ProtocolState.PLAY, Direction.S2C, "entity_position_sync", positionSync(900));
        feed(engine, 2 * SECOND, ProtocolState.PLAY, Direction.S2C, "entity_position_sync", positionSync(901));
        clock.set(3 * SECOND);
        engine.stop(TraceHeader.ClosedBy.STOP);

        var trace = TraceReader.read(traces.take().path());
        var ids = trace.frames().stream()
            .map(frame -> entityIdOf(trace.header(), frame))
            .filter(id -> id != null)
            .toList();
        assertEquals(List.of(901, 901), ids, "the display entity's add and sync are both left out");

        engine.close();
    }

    /// The entity a frame is keyed on, for the entity packets this test feeds; null for the rest.
    private static @Nullable Integer entityIdOf(TraceHeader header, Frame frame) {
        var name = Protocol776.lookup(frame.state(), frame.direction(), frame.packetId()).name();
        return switch (name) {
            case "add_entity" -> S2CAddEntity.V776.decode(new ByteReader(frame.bytes())).entityId();
            case "entity_position_sync" -> S2CEntityPositionSync.V776.decode(new ByteReader(frame.bytes())).entityId();
            default -> null;
        };
    }

    private static byte[] sections(S2CLevelChunkWithLight.V776 chunk) {
        var writer = new ByteWriter();
        for (var section : chunk.sections()) section.encode(writer);
        return writer.toByteArray();
    }

    private CaptureEngine engine(TestCapture.ManualClock clock, TestCapture.Traces traces) {
        return new CaptureEngine(TestCapture.config(directory), TestCapture.identity(), () -> null, clock, traces);
    }

    /// The frames every connection starts with, so the model has a player and a world to snapshot.
    private static void join(CaptureEngine engine, TestCapture.ManualClock clock, long tNs) {
        clock.set(tNs);
        feed(engine, tNs, ProtocolState.PLAY, Direction.S2C, "login", login());
        feed(engine, tNs, ProtocolState.PLAY, Direction.S2C, "set_chunk_cache_radius", viewDistance(32));
    }

    private void assertSpoolEmpty() throws IOException {
        var spool = directory.resolve("spool");
        if (!Files.isDirectory(spool)) return;
        try (Stream<Path> files = Files.list(spool)) {
            var left = files.toList();
            assertEquals(List.of(), left, "the spool was left behind");
        }
    }
}
