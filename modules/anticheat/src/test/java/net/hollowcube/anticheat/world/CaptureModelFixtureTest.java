package net.hollowcube.anticheat.world;

import net.hollowcube.anticheat.protocol.*;
import net.hollowcube.anticheat.state.StateCache;
import net.hollowcube.anticheat.state.StateCacheView;
import net.hollowcube.anticheat.state.StateKey;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/// Replays the checked-in real-client 776 captures through the world model and the state cache.
///
/// The chunk count is checked against a second, independent piece of bookkeeping: a set of the
/// positions that were loaded, minus what was forgotten, cleared or moved out of the ring. That
/// catches a model that quietly loses or duplicates chunks without re-deriving the model itself.
///
/// The fixtures are optional; without them the test is skipped rather than silently passing.
class CaptureModelFixtureTest {

    @Test
    void testFixturesReplayThroughTheWorldAndStateModels() {
        var fixtures = fixtures();
        Assumptions.assumeFalse(fixtures.isEmpty(), "no 776 capture fixtures present");

        for (var fixture : fixtures) {
            var capture = FixtureReader.read(fixture);
            var source = fixture.getFileName().toString();

            var world = new ChunkMap();
            var state = new StateCache();
            var loaded = new HashSet<Long>();
            int chunkFrames = 0;

            for (var frame : capture.frames()) {
                var entry = Protocol776.lookup(frame.state(), frame.direction(), frame.packetId());
                if (!entry.kept()) continue;

                var packet = decode(entry, frame);
                state.apply(frame.state(), frame.direction(), frame.packetId(), frame.body(), packet);
                if (packet == null || frame.direction() != Direction.S2C) continue;

                world.handle(packet);
                if (packet instanceof S2CLevelChunkWithLight chunk) chunkFrames++;
                track(loaded, world, packet);
            }

            assertTrue(chunkFrames > 0, source + " carried no chunks");
            assertEquals(loaded.size(), world.chunkCount(), source + " chunk count");
            assertTrue(world.chunkCount() > 0, source + " ended with no chunks");

            var view = state.snapshot();
            assertTrue(view.keyCount() > 0, source + " cached no state");
            assertEquals(view.frames().size(), new HashSet<>(view.frames()).size(), source + " duplicated a frame");
            assertEquals(List.of(), view.frames(new StateKey.PlayerInfo(null)),
                source + " fell back to the whole-packet bucket for a player_info_update");
            for (Chunk chunk : world.snapshot().loadedChunks())
                assertEquals(world.dimension().sectionCount(), chunk.sectionCount(), source);

            assertChunksKeepTheirSections(capture, source);
        }
    }

    /// Every chunk that arrived, replayed on its own so no block update has touched it, has to hold
    /// the exact sections it was sent. The block states of a trace's start state are never lossy.
    private void assertChunksKeepTheirSections(FixtureReader capture, String source) {
        var world = new ChunkMap();
        var received = new HashMap<Long, byte[]>();
        int checked = 0;

        for (var frame : capture.frames()) {
            var entry = Protocol776.lookup(frame.state(), frame.direction(), frame.packetId());
            if (!entry.kept() || frame.direction() != Direction.S2C) continue;
            var packet = decode(entry, frame);
            if (packet == null) continue;
            if (packet instanceof S2CLevelChunkWithLight chunk)
                received.put(Positions.chunkPos(chunk.chunkX(), chunk.chunkZ()), encode(chunk.sections()));
            if (packet instanceof S2CLevelChunkWithLight || packet instanceof S2CLogin || packet instanceof S2CRespawn
                || packet instanceof S2CStartConfiguration || packet instanceof S2CSetChunkCacheCenter
                || packet instanceof S2CSetChunkCacheRadius || packet instanceof S2CForgetLevelChunk)
                world.handle(packet);
        }

        for (var chunk : world.snapshot().loadedChunks()) {
            byte[] original = received.get(Positions.chunkPos(chunk.chunkX(), chunk.chunkZ()));
            assertArrayEquals(original, encode(chunk.sections()),
                source + " chunk " + chunk.chunkX() + "," + chunk.chunkZ() + " kept different sections");
            checked++;
        }
        assertTrue(checked > 0, source + " checked no chunk sections");
    }

    private static byte[] encode(List<Section> sections) {
        var writer = new ByteWriter();
        for (Section section : sections) section.encode(writer);
        return writer.toByteArray();
    }

    /// The independent chunk bookkeeping, kept as a plain set of positions.
    private static void track(Set<Long> loaded, ChunkMap world, Packet packet) {
        switch (packet) {
            case S2CLevelChunkWithLight chunk -> {
                if (world.chunk(chunk.chunkX(), chunk.chunkZ()) != null)
                    loaded.add(Positions.chunkPos(chunk.chunkX(), chunk.chunkZ()));
            }
            case S2CForgetLevelChunk chunk -> loaded.remove(Positions.chunkPos(chunk.chunkX(), chunk.chunkZ()));
            case S2CSetChunkCacheCenter _, S2CSetChunkCacheRadius _ -> dropOutOfRange(loaded, world);
            case S2CLogin _, S2CStartConfiguration _ -> loaded.clear();
            case S2CRespawn _ -> {
                if (world.chunkCount() == 0) loaded.clear();
            }
            default -> {
            }
        }
    }

    private static void dropOutOfRange(Set<Long> loaded, ChunkMap world) {
        loaded.removeIf(pos -> Math.abs(Positions.chunkX(pos) - world.viewCenterX()) > world.storageRadius()
            || Math.abs(Positions.chunkZ(pos) - world.viewCenterZ()) > world.storageRadius());
    }

    private static @Nullable Packet decode(Protocol776.Entry entry, FixtureReader.Frame frame) {
        var decoder = entry.decoder();
        return decoder == null ? null : decoder.decode(new ByteReader(frame.body()));
    }

    private static List<Path> fixtures() {
        var directory = CaptureModelFixtureTest.class.getResource("/fixtures/" + Protocol776.PROTOCOL_VERSION);
        if (directory == null) return List.of();
        try (Stream<Path> files = Files.list(Path.of(directory.toURI()))) {
            var result = new ArrayList<Path>(files.filter(f -> f.toString().endsWith(".hcpt.zst")).toList());
            result.sort(Path::compareTo);
            return List.copyOf(result);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
