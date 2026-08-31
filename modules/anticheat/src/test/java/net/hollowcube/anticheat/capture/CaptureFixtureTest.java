package net.hollowcube.anticheat.capture;

import net.hollowcube.anticheat.log.*;
import net.hollowcube.anticheat.protocol.*;
import net.hollowcube.anticheat.state.StateCache;
import net.hollowcube.anticheat.state.TrackedEntity;
import net.hollowcube.anticheat.world.ChunkMap;
import net.hollowcube.anticheat.world.WorldView;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/// Replays the checked-in real-client 776 captures through the whole engine and reads the traces
/// back, which is the only test here that sees the model, the ring, the trim and the writer at once.
///
/// What a trace carries is checked against a second replay through [ChunkMap] and [StateCache]
/// alone: a capture that opens before the login carries no world at all (every chunk arrives as a
/// frame), one that opens at the end carries exactly what the client had, and one that opens in the
/// middle carries the trim of it.
///
/// The fixtures are optional; without them the test is skipped rather than silently passing.
class CaptureFixtureTest {

    @TempDir
    Path directory;

    @Test
    void testACaptureFromTheFirstFrameCarriesEveryFrameAndNoWorld() throws Exception {
        for (var fixture : fixtures()) {
            var source = fixture.getFileName().toString();
            var capture = FixtureReader.read(fixture);
            var traces = new TestCapture.Traces();
            var clock = new TestCapture.ManualClock();
            var engine = engine(source, clock, traces);

            clock.set(capture.frames().getFirst().tNs());
            engine.start("fixture-" + source, TraceHeader.Reason.RUN, null, TrimPolicy.EVERYTHING);
            replay(engine, clock, capture.frames());
            engine.stop(TraceHeader.ClosedBy.STOP);

            var written = traces.take();
            var header = written.header();
            assertEquals(kept(capture.frames(), 0), header.counters().frames(), source);
            assertEquals(0, header.counters().droppedFrames(), source);
            assertEquals(TraceHeader.Flags.NONE, header.flags(), source);
            // Nothing had happened yet, so the start state is empty and the whole session replays.
            assertEquals(0, header.counters().chunks(), source);
            assertEquals(0, header.counters().preludeFrames(), source);

            var trace = TraceReader.read(written.path());
            assertFalse(trace.truncated(), source);
            assertEquals(header.counters().frames(), trace.frames().size(), source);
            // Relative to the snapshot the capture opened at, which is the first frame of the file
            // whether or not that one was kept.
            long base = capture.frames().getFirst().tNs();
            assertTrue(trace.frames().getFirst().tNs() >= 0, source);
            assertEquals(lastKeptNs(capture.frames()) - base, trace.frames().getLast().tNs(), source);

            var dump = new StringBuilder();
            Dump.dump(written.path(), false, dump);
            assertTrue(dump.toString().contains("frames          " + header.counters().frames()), source);

            engine.close();
        }
    }

    @Test
    void testACaptureAtTheEndCarriesEveryChunkTheClientHad() throws Exception {
        for (var fixture : fixtures()) {
            var source = fixture.getFileName().toString();
            var capture = FixtureReader.read(fixture);
            var traces = new TestCapture.Traces();
            var clock = new TestCapture.ManualClock();
            var engine = engine(source, clock, traces);

            replay(engine, clock, capture.frames());
            engine.start("fixture-" + source, TraceHeader.Reason.RUN, null, TrimPolicy.EVERYTHING);
            engine.stop(TraceHeader.ClosedBy.STOP);

            var header = traces.take().header();
            var expected = replay(capture, capture.frames().size(), TrimPolicy.EVERYTHING);
            assertTrue(expected.chunks() > 0, source + " loaded no chunks");
            assertEquals(expected.chunks(), header.counters().chunks(), source);
            assertEquals(0, header.counters().frames(), source);
            assertTrue(header.counters().preludeFrames() > 0, source + " cached no state");

            engine.close();
        }
    }

    @Test
    void testACaptureFromTheMiddleCarriesTheTrimOfWhatTheClientHad() throws Exception {
        for (var fixture : fixtures()) {
            var source = fixture.getFileName().toString();
            var capture = FixtureReader.read(fixture);
            var frames = capture.frames();
            int split = frames.size() / 2;
            var policy = TrimPolicy.DEFAULT;

            var traces = new TestCapture.Traces();
            var clock = new TestCapture.ManualClock();
            var engine = engine(source, clock, traces);

            replay(engine, clock, frames.subList(0, split));
            clock.set(frames.get(split).tNs());
            engine.start("fixture-" + source, TraceHeader.Reason.RUN, null, policy);
            replay(engine, clock, frames.subList(split, frames.size()));
            engine.stop(TraceHeader.ClosedBy.STOP);

            var header = traces.take().header();
            var expected = replay(capture, split, policy);
            assertTrue(expected.chunks() > 0, source + " trimmed to nothing");
            assertEquals(expected.chunks(), header.counters().chunks(), source);
            assertEquals(kept(frames, split), header.counters().frames(), source);
            assertTrue(expected.chunks() < expected.loaded(),
                source + " trimmed nothing away, so the trim proves nothing");

            engine.close();
        }
    }

    private CaptureEngine engine(String source, TestCapture.ManualClock clock, TestCapture.Traces traces) {
        return new CaptureEngine(TestCapture.config(directory.resolve(source.replace('.', '_'))),
            TestCapture.identity(), () -> null, clock, traces);
    }

    private static void replay(CaptureEngine engine, TestCapture.ManualClock clock, List<FixtureReader.Frame> frames) {
        for (var frame : frames) {
            clock.set(frame.tNs());
            engine.frame(frame.tNs(), frame.direction(), frame.state(), frame.packetId(), Frame.NO_PING, frame.body());
        }
    }

    /// What the engine will store from `from` on: the table says the packet is kept, and it is not
    /// a frame for a display entity, which the engine leaves out the way the model, the trim and
    /// the prelude already do. The whole fixture is replayed either way, because whether an entity
    /// is a display is only known from the `add_entity` that may precede `from`.
    private static long kept(List<FixtureReader.Frame> frames, int from) {
        var state = new StateCache();
        long kept = 0;
        for (int i = 0; i < frames.size(); i++) {
            var frame = frames.get(i);
            var entry = Protocol776.lookup(frame.state(), frame.direction(), frame.packetId());
            if (!entry.kept()) continue;

            var decoder = entry.decoder();
            var packet = decoder == null ? null : decoder.decode(new ByteReader(frame.body()));
            state.apply(frame.state(), frame.direction(), frame.packetId(), frame.body(), packet);
            if (i < from) continue;
            if (packet instanceof EntityKeyed keyed && state.entities().isDropped(keyed.entityId())) continue;
            kept++;
        }
        return kept;
    }

    /// What the capture should have carried, from the model alone: the trimmed chunk count and how
    /// many chunks were loaded at the point the capture opened.
    private static long lastKeptNs(List<FixtureReader.Frame> frames) {
        var state = new StateCache();
        long last = Long.MIN_VALUE;
        for (var frame : frames) {
            var entry = Protocol776.lookup(frame.state(), frame.direction(), frame.packetId());
            if (!entry.kept()) continue;

            var decoder = entry.decoder();
            var packet = decoder == null ? null : decoder.decode(new ByteReader(frame.body()));
            state.apply(frame.state(), frame.direction(), frame.packetId(), frame.body(), packet);
            if (packet instanceof EntityKeyed keyed && state.entities().isDropped(keyed.entityId())) continue;
            last = frame.tNs();
        }
        if (last == Long.MIN_VALUE) throw new IllegalStateException("the fixture kept no frames at all");
        return last;
    }

    private record Replay(int chunks, int loaded) {
    }

    /// The same replay the engine does, without the engine: the world and the state at `split`, the
    /// chunks of interest after it, and the region the two produce.
    private static Replay replay(FixtureReader capture, int split, TrimPolicy policy) {
        var world = new ChunkMap();
        var state = new StateCache();
        var trim = new Trim();
        var frames = capture.frames();
        WorldView view = null;
        long startNs = 0;

        for (int i = 0; i < frames.size(); i++) {
            if (i == split) {
                view = world.snapshot();
                startNs = frames.get(i).tNs();
                note(trim, state, startNs);
            }
            var frame = frames.get(i);
            var entry = Protocol776.lookup(frame.state(), frame.direction(), frame.packetId());
            if (!entry.kept()) continue;

            var decoder = entry.decoder();
            var packet = decoder == null ? null : decoder.decode(new ByteReader(frame.body()));
            state.apply(frame.state(), frame.direction(), frame.packetId(), frame.body(), packet);
            if (packet == null) continue;
            if (frame.direction() == Direction.S2C) world.handle(packet);
            note(trim, state, frame.tNs(), packet, policy);
        }
        if (view == null) {
            view = world.snapshot();
            startNs = frames.getLast().tNs();
            note(trim, state, startNs);
        }
        return new Replay(Trim.region(view, policy, trim.since(startNs)).size(), view.chunkCount());
    }

    private static void note(Trim trim, StateCache state, long tNs) {
        var player = state.entities().player();
        if (player.entityId() >= 0) trim.add(tNs, player.x(), player.z());
    }

    private static void note(Trim trim, StateCache state, long tNs, Packet packet, TrimPolicy policy) {
        var player = state.entities().player();
        switch (packet) {
            case MovePlayer _, S2CPlayerPosition _ -> trim.add(tNs, player.x(), player.z());
            case S2CAddEntity entity -> note(trim, state, tNs, entity.entityId(), player, policy);
            case MoveEntity entity -> note(trim, state, tNs, entity.entityId(), player, policy);
            case S2CTeleportEntity entity -> note(trim, state, tNs, entity.entityId(), player, policy);
            case S2CEntityPositionSync entity -> note(trim, state, tNs, entity.entityId(), player, policy);
            default -> {
            }
        }
    }

    private static void note(Trim trim, StateCache state, long tNs, int entityId, TrackedEntity player,
                             TrimPolicy policy) {
        var entity = state.entities().get(entityId);
        if (entity == null || entity.dropped()) return;
        double range = policy.entityRange();
        double dx = entity.x() - player.x();
        double dy = entity.y() - player.y();
        double dz = entity.z() - player.z();
        if (dx * dx + dy * dy + dz * dz > range * range) return;
        trim.add(tNs, entity.x(), entity.z());
    }

    private static List<Path> fixtures() {
        var directory = CaptureFixtureTest.class.getResource("/fixtures/" + Protocol776.PROTOCOL_VERSION);
        Assumptions.assumeTrue(directory != null, "no 776 capture fixtures present");
        try (Stream<Path> files = Files.list(Path.of(directory.toURI()))) {
            var result = new ArrayList<Path>(files.filter(file -> file.toString().endsWith(".hcpt.zst")).toList());
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
